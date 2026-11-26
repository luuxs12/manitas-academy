package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.ContrasenaChangeFormulario;
import com.mundomanitas.api.dto.PerfilFormulario;
import com.mundomanitas.api.exception.DuplicadoEmailException;
import com.mundomanitas.api.exception.ContrasenaNoCoincideException;
import com.mundomanitas.api.exception.IncorrectaContrasenaException;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Plan;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CuentaService;
import com.mundomanitas.api.service.InscripcionService;
import com.mundomanitas.api.service.LeccionProgresoService;
import com.mundomanitas.api.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Módulo de perfil: datos personales, foto, contraseña y tipo de cuenta.
 *
 * <p>El plan ya no se cambia a mano: solo se activa cuando Stripe confirma
 * el cobro. Los números de la sección salen de PostgreSQL.</p>
 */
@Controller
@RequestMapping("/panel")
public class PerfilController {

    private final CuentaService accounts;
    private final InscripcionService enrollments;
    private final LeccionProgresoService lessons;
    private final PagoService payments;

    public PerfilController(
            CuentaService accounts,
            InscripcionService enrollments,
            LeccionProgresoService lessons,
            PagoService payments
    ) {
        this.accounts = accounts;
        this.enrollments = enrollments;
        this.lessons = lessons;
        this.payments = payments;
    }

    @GetMapping("/perfil")
    public String perfil(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            Model model
    ) {
        Usuario usuario = accounts.require(principal.getId());
        if (!model.containsAttribute("perfilFormulario")) {
            model.addAttribute("perfilFormulario", formularioFor(usuario));
        }
        return perfilVista(usuario, model);
    }

    @PostMapping("/perfil")
    public String updatePerfil(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @Valid @ModelAttribute("perfilFormulario") PerfilFormulario formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect
    ) {
        Usuario usuario = accounts.require(principal.getId());
        if (!resultado.hasErrors()) {
            try {
                accounts.updatePerfil(usuario, formulario);
                redirect.addFlashAttribute("panelOk", "Tus datos se guardaron.");
                return "redirect:/panel/perfil";
            } catch (DuplicadoEmailException exception) {
                resultado.rejectValue("email", "duplicate", "Ese correo ya está registrado.");
            }
        }
        return perfilVista(usuario, model);
    }

    @PostMapping("/perfil/contrasena")
    public String changeContrasena(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @Valid @ModelAttribute("contrasenaFormulario") ContrasenaChangeFormulario formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect
    ) {
        Usuario usuario = accounts.require(principal.getId());
        if (!resultado.hasErrors()) {
            try {
                accounts.changeContrasena(usuario, formulario);
                redirect.addFlashAttribute("panelOk", "Tu contraseña se actualizó.");
                return "redirect:/panel/perfil#contrasena";
            } catch (IncorrectaContrasenaException exception) {
                resultado.rejectValue(
                        "currentPassword", "wrong", "La contraseña actual no es correcta.");
            } catch (ContrasenaNoCoincideException exception) {
                resultado.rejectValue(
                        "confirmPassword", "mismatch", "Las contraseñas nuevas no coinciden.");
            }
        }
        model.addAttribute("perfilFormulario", formularioFor(usuario));
        return perfilVista(usuario, model);
    }

    @PostMapping("/perfil/foto")
    public String cargaAvatar(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @RequestParam("avatar") MultipartFile avatar,
            RedirectAttributes redirect
    ) {
        Usuario usuario = accounts.require(principal.getId());
        try {
            accounts.updateAvatar(usuario, avatar);
            redirect.addFlashAttribute("panelOk", "Tu foto de perfil se actualizó.");
        } catch (ResponseStatusException exception) {
            redirect.addFlashAttribute("panelError", exception.getReason());
        }
        return "redirect:/panel/perfil#foto";
    }

    @PostMapping("/perfil/foto/eliminar")
    public String removeAvatar(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            RedirectAttributes redirect
    ) {
        accounts.removeAvatar(accounts.require(principal.getId()));
        redirect.addFlashAttribute("panelOk", "Volviste a usar tus iniciales.");
        return "redirect:/panel/perfil#foto";
    }

    private String perfilVista(Usuario usuario, Model model) {
        if (!model.containsAttribute("contrasenaFormulario")) {
            model.addAttribute("contrasenaFormulario", new ContrasenaChangeFormulario());
        }

        List<Inscripcion> mine = enrollments.mine(usuario);
        model.addAttribute("planes", Plan.purchasable());
        model.addAttribute("pagoEnabled", payments.isEnabled());
        model.addAttribute("inscripto", mine.size());
        model.addAttribute("finalizado", enrollments.finalizado(usuario).size());
        model.addAttribute("videoTotals", lessons.totals(mine));
        Locale spanish = Locale.forLanguageTag("es");
        model.addAttribute("memberSinceLabel", usuario.getFechaCreacion() == null ? "" :
                usuario.getFechaCreacion().format(DateTimeFormatter.ofPattern("MMMM yyyy", spanish)));
        model.addAttribute("subscriptionFinLabel", usuario.getSubscriptionFin() == null ? "" :
                usuario.getSubscriptionFin().format(
                        DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", spanish)));
        model.addAttribute("cancelAtPeriodFin", usuario.isCancelAtPeriodFin());
        return "student/perfil";
    }

    private PerfilFormulario formularioFor(Usuario usuario) {
        PerfilFormulario formulario = new PerfilFormulario();
        formulario.setNombreCompleto(usuario.getNombreCompleto());
        formulario.setEmail(usuario.getEmail());
        return formulario;
    }
}
