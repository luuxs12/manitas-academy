package com.mundomanitas.api.controller;

import com.mundomanitas.api.exception.PagoNoDisponibleException;
import com.mundomanitas.api.model.Plan;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CuentaService;
import com.mundomanitas.api.service.PagoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Suscripción e historial de cobros.
 *
 * <p>El plan cambia solo desde el webhook de Stripe o desde la confirmación
 * del checkout; acá nunca se escribe el plan directamente.</p>
 */
@Controller
@RequestMapping("/panel/facturacion")
public class FacturacionController {

    private final CuentaService accounts;
    private final PagoService payments;

    public FacturacionController(CuentaService accounts, PagoService payments) {
        this.accounts = accounts;
        this.payments = payments;
    }

    @GetMapping
    public String history(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            Model model
    ) {
        Usuario usuario = accounts.require(principal.getId());
        var history = payments.history(usuario);
        Locale spanish = Locale.forLanguageTag("es");

        model.addAttribute("payments", history);
        model.addAttribute("planes", Plan.purchasable());
        model.addAttribute("pagoEnabled", payments.isEnabled());
        model.addAttribute("embeddedEnabled", payments.isEmbeddedEnabled());
        model.addAttribute("publishableClave", payments.publishableClave());
        model.addAttribute("portalUrl", payments.customerPortalUrl());
        model.addAttribute("subscriptionFinLabel", usuario.getSubscriptionFin() == null ? "" :
                usuario.getSubscriptionFin().format(
                        DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", spanish)));
        model.addAttribute("cancelAtPeriodFin", usuario.isCancelAtPeriodFin());
        model.addAttribute("canCancel", usuario.isPremium() && history.stream()
                .anyMatch(pago -> pago.getStripeSubscriptionId() != null));
        return "student/facturacion";
    }

    @PostMapping("/checkout")
    public String checkout(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @RequestParam("plan") Plan plan,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        Usuario usuario = accounts.require(principal.getId());
        String base = ServletUriComponentsBuilder.fromRequest(request)
                .replaceQuery(null)
                .replacePath("")
                .build()
                .toUriString();

        try {
            String url = payments.checkout(
                    usuario,
                    plan,
                    base + "/panel/facturacion/resultado",
                    base + "/panel/perfil#plan"
            );
            return "redirect:" + url;
        } catch (PagoNoDisponibleException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/panel/facturacion";
        }
    }

    /**
     * Crea la sesión de Checkout embebido y devuelve el client_secret para que
     * Stripe.js monte el formulario dentro de esta misma página.
     */
    @PostMapping(value = "/checkout/embebido", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> embeddedCheckout(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @RequestParam("plan") Plan plan,
            HttpServletRequest request
    ) {
        Usuario usuario = accounts.require(principal.getId());
        String base = ServletUriComponentsBuilder.fromRequest(request)
                .replaceQuery(null)
                .replacePath("")
                .build()
                .toUriString();
        String returnUrl = base
                + "/panel/facturacion/resultado?session_id={CHECKOUT_SESSION_ID}";

        try {
            PagoService.EmbeddedCheckout checkout =
                    payments.embeddedCheckout(usuario, plan, returnUrl);
            return ResponseEntity.ok(Map.of(
                    "clientSecret", checkout.clientSecret(),
                    "sessionId", checkout.sesionId(),
                    "publishableKey", payments.publishableClave()
            ));
        } catch (PagoNoDisponibleException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/resultado")
    public String returned(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @RequestParam(name = "session_id", required = false) String sesionId,
            RedirectAttributes redirect
    ) {
        try {
            payments.confirmDespuesReturn(accounts.require(principal.getId()), sesionId);
            redirect.addFlashAttribute(
                    "panelOk", "Estamos confirmando tu pago. El plan se activa apenas llegue la confirmación.");
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute(
                    "panelError", "No pudimos confirmar el pago con Stripe. Revisá el historial.");
        }
        return "redirect:/panel/facturacion";
    }

    @PostMapping("/cancelar")
    public String cancel(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            RedirectAttributes redirect
    ) {
        try {
            payments.cancelAtPeriodFin(accounts.require(principal.getId()));
            redirect.addFlashAttribute(
                    "panelOk", "Tu suscripción se cancela al final del período pagado.");
        } catch (PagoNoDisponibleException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/panel/facturacion";
    }
}
