package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.ForgotPasswordForm;
import com.mundomanitas.api.dto.ResetPasswordForm;
import com.mundomanitas.api.exception.InvalidResetTokenException;
import com.mundomanitas.api.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Pantallas de recuperación de contraseña.
 *
 * <p>El pedido responde siempre lo mismo, exista o no la cuenta, para no
 * revelar correos registrados.</p>
 */
@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/recuperar")
    public String forgot(
            @RequestParam(name = "enviado", required = false) String sent,
            Model model
    ) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ForgotPasswordForm());
        }
        model.addAttribute("sent", sent != null);
        return "auth/forgot-password";
    }

    @PostMapping("/recuperar")
    public String forgot(
            @Valid @ModelAttribute("form") ForgotPasswordForm form,
            BindingResult result
    ) {
        if (result.hasErrors()) {
            return "auth/forgot-password";
        }

        passwordResetService.requestReset(form.getEmail());
        return "redirect:/recuperar?enviado";
    }

    @GetMapping("/restablecer")
    public String reset(
            @RequestParam(name = "token", required = false) String token,
            Model model
    ) {
        boolean usable = passwordResetService.isUsable(token);

        if (!usable) {
            model.addAttribute("invalid", true);
            return "auth/reset-password";
        }

        if (!model.containsAttribute("form")) {
            ResetPasswordForm form = new ResetPasswordForm();
            form.setToken(token);
            model.addAttribute("form", form);
        }
        model.addAttribute("invalid", false);
        return "auth/reset-password";
    }

    @PostMapping("/restablecer")
    public String reset(
            @Valid @ModelAttribute("form") ResetPasswordForm form,
            BindingResult result,
            Model model
    ) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Las contraseñas no coinciden."
            );
        }

        if (result.hasErrors()) {
            model.addAttribute("invalid", false);
            return "auth/reset-password";
        }

        try {
            passwordResetService.resetPassword(form.getToken(), form.getPassword());
        } catch (InvalidResetTokenException exception) {
            model.addAttribute("invalid", true);
            return "auth/reset-password";
        }

        return "redirect:/login?reset";
    }
}
