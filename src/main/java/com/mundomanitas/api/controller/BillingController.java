package com.mundomanitas.api.controller;

import com.mundomanitas.api.exception.PaymentUnavailableException;
import com.mundomanitas.api.model.Plan;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.AccountService;
import com.mundomanitas.api.service.PaymentService;
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
public class BillingController {

    private final AccountService accounts;
    private final PaymentService payments;

    public BillingController(AccountService accounts, PaymentService payments) {
        this.accounts = accounts;
        this.payments = payments;
    }

    @GetMapping
    public String history(
            @AuthenticationPrincipal AuthenticatedUser principal,
            Model model
    ) {
        User user = accounts.require(principal.getId());
        var history = payments.history(user);
        Locale spanish = Locale.forLanguageTag("es");

        model.addAttribute("payments", history);
        model.addAttribute("planes", Plan.purchasable());
        model.addAttribute("paymentEnabled", payments.isEnabled());
        model.addAttribute("embeddedEnabled", payments.isEmbeddedEnabled());
        model.addAttribute("publishableKey", payments.publishableKey());
        model.addAttribute("portalUrl", payments.customerPortalUrl());
        model.addAttribute("subscriptionEndLabel", user.getSubscriptionEnd() == null ? "" :
                user.getSubscriptionEnd().format(
                        DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", spanish)));
        model.addAttribute("cancelAtPeriodEnd", user.isCancelAtPeriodEnd());
        model.addAttribute("canCancel", user.isPremium() && history.stream()
                .anyMatch(payment -> payment.getStripeSubscriptionId() != null));
        return "student/facturacion";
    }

    @PostMapping("/checkout")
    public String checkout(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam("plan") Plan plan,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        User user = accounts.require(principal.getId());
        String base = ServletUriComponentsBuilder.fromRequest(request)
                .replaceQuery(null)
                .replacePath("")
                .build()
                .toUriString();

        try {
            String url = payments.checkout(
                    user,
                    plan,
                    base + "/panel/facturacion/resultado",
                    base + "/panel/perfil#plan"
            );
            return "redirect:" + url;
        } catch (PaymentUnavailableException exception) {
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
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam("plan") Plan plan,
            HttpServletRequest request
    ) {
        User user = accounts.require(principal.getId());
        String base = ServletUriComponentsBuilder.fromRequest(request)
                .replaceQuery(null)
                .replacePath("")
                .build()
                .toUriString();
        String returnUrl = base
                + "/panel/facturacion/resultado?session_id={CHECKOUT_SESSION_ID}";

        try {
            PaymentService.EmbeddedCheckout checkout =
                    payments.embeddedCheckout(user, plan, returnUrl);
            return ResponseEntity.ok(Map.of(
                    "clientSecret", checkout.clientSecret(),
                    "sessionId", checkout.sessionId(),
                    "publishableKey", payments.publishableKey()
            ));
        } catch (PaymentUnavailableException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/resultado")
    public String returned(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(name = "session_id", required = false) String sessionId,
            RedirectAttributes redirect
    ) {
        try {
            payments.confirmAfterReturn(accounts.require(principal.getId()), sessionId);
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
            @AuthenticationPrincipal AuthenticatedUser principal,
            RedirectAttributes redirect
    ) {
        try {
            payments.cancelAtPeriodEnd(accounts.require(principal.getId()));
            redirect.addFlashAttribute(
                    "panelOk", "Tu suscripción se cancela al final del período pagado.");
        } catch (PaymentUnavailableException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/panel/facturacion";
    }
}
