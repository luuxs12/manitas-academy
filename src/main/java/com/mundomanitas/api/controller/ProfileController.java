package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.PasswordChangeForm;
import com.mundomanitas.api.dto.ProfileForm;
import com.mundomanitas.api.exception.DuplicateEmailException;
import com.mundomanitas.api.exception.PasswordMismatchException;
import com.mundomanitas.api.exception.WrongPasswordException;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.Plan;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.AccountService;
import com.mundomanitas.api.service.EnrollmentService;
import com.mundomanitas.api.service.LessonProgressService;
import com.mundomanitas.api.service.PaymentService;
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
public class ProfileController {

    private final AccountService accounts;
    private final EnrollmentService enrollments;
    private final LessonProgressService lessons;
    private final PaymentService payments;

    public ProfileController(
            AccountService accounts,
            EnrollmentService enrollments,
            LessonProgressService lessons,
            PaymentService payments
    ) {
        this.accounts = accounts;
        this.enrollments = enrollments;
        this.lessons = lessons;
        this.payments = payments;
    }

    @GetMapping("/perfil")
    public String profile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            Model model
    ) {
        User user = accounts.require(principal.getId());
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", formFor(user));
        }
        return profileView(user, model);
    }

    @PostMapping("/perfil")
    public String updateProfile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @ModelAttribute("profileForm") ProfileForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirect
    ) {
        User user = accounts.require(principal.getId());
        if (!result.hasErrors()) {
            try {
                accounts.updateProfile(user, form);
                redirect.addFlashAttribute("panelOk", "Tus datos se guardaron.");
                return "redirect:/panel/perfil";
            } catch (DuplicateEmailException exception) {
                result.rejectValue("email", "duplicate", "Ese correo ya está registrado.");
            }
        }
        return profileView(user, model);
    }

    @PostMapping("/perfil/contrasena")
    public String changePassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirect
    ) {
        User user = accounts.require(principal.getId());
        if (!result.hasErrors()) {
            try {
                accounts.changePassword(user, form);
                redirect.addFlashAttribute("panelOk", "Tu contraseña se actualizó.");
                return "redirect:/panel/perfil#contrasena";
            } catch (WrongPasswordException exception) {
                result.rejectValue(
                        "currentPassword", "wrong", "La contraseña actual no es correcta.");
            } catch (PasswordMismatchException exception) {
                result.rejectValue(
                        "confirmPassword", "mismatch", "Las contraseñas nuevas no coinciden.");
            }
        }
        model.addAttribute("profileForm", formFor(user));
        return profileView(user, model);
    }

    @PostMapping("/perfil/foto")
    public String uploadAvatar(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam("avatar") MultipartFile avatar,
            RedirectAttributes redirect
    ) {
        User user = accounts.require(principal.getId());
        try {
            accounts.updateAvatar(user, avatar);
            redirect.addFlashAttribute("panelOk", "Tu foto de perfil se actualizó.");
        } catch (ResponseStatusException exception) {
            redirect.addFlashAttribute("panelError", exception.getReason());
        }
        return "redirect:/panel/perfil#foto";
    }

    @PostMapping("/perfil/foto/eliminar")
    public String removeAvatar(
            @AuthenticationPrincipal AuthenticatedUser principal,
            RedirectAttributes redirect
    ) {
        accounts.removeAvatar(accounts.require(principal.getId()));
        redirect.addFlashAttribute("panelOk", "Volviste a usar tus iniciales.");
        return "redirect:/panel/perfil#foto";
    }

    private String profileView(User user, Model model) {
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
        }

        List<Enrollment> mine = enrollments.mine(user);
        model.addAttribute("planes", Plan.purchasable());
        model.addAttribute("paymentEnabled", payments.isEnabled());
        model.addAttribute("enrolled", mine.size());
        model.addAttribute("completed", enrollments.completed(user).size());
        model.addAttribute("videoTotals", lessons.totals(mine));
        Locale spanish = Locale.forLanguageTag("es");
        model.addAttribute("memberSinceLabel", user.getCreatedAt() == null ? "" :
                user.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM yyyy", spanish)));
        model.addAttribute("subscriptionEndLabel", user.getSubscriptionEnd() == null ? "" :
                user.getSubscriptionEnd().format(
                        DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", spanish)));
        model.addAttribute("cancelAtPeriodEnd", user.isCancelAtPeriodEnd());
        return "student/perfil";
    }

    private ProfileForm formFor(User user) {
        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        return form;
    }
}
