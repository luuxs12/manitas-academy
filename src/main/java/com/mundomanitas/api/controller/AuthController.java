package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.RegisterForm;
import com.mundomanitas.api.exception.DuplicateEmailException;
import com.mundomanitas.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pantallas y acciones de acceso.
 *
 * <p>El POST de login lo atiende Spring Security; acá solo se muestra el
 * formulario y se reciben los avisos de error, registro o cierre de sesión.</p>
 */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login(
            Authentication authentication,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            Model model
    ) {
        if (isSignedIn(authentication)) {
            return "redirect:/";
        }

        model.addAttribute("loginError", error != null);
        model.addAttribute("loggedOut", logout != null);
        return "auth/login";
    }

    @GetMapping("/registro")
    public String register(Authentication authentication, Model model) {
        if (isSignedIn(authentication)) {
            return "redirect:/";
        }

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/registro")
    public String register(
            @Valid @ModelAttribute("form") RegisterForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes
    ) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Las contraseñas no coinciden."
            );
        }

        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.register(form);
        } catch (DuplicateEmailException | DataIntegrityViolationException exception) {
            result.rejectValue(
                    "email",
                    "email.duplicate",
                    "Ya existe una cuenta con ese correo."
            );
            return "auth/register";
        }

        redirectAttributes.addFlashAttribute("registered", true);
        redirectAttributes.addFlashAttribute("registeredEmail", form.getEmail().trim());
        return "redirect:/login";
    }

    private boolean isSignedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
