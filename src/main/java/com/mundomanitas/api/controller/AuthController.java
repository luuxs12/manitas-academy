package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.RegisterForm;
import com.mundomanitas.api.exception.DuplicateEmailException;
import com.mundomanitas.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Pantallas y acciones de acceso.
 *
 * <p>El POST de login lo atiende Spring Security; acá solo se muestra el
 * formulario y se reciben los avisos de error, registro o cierre de sesión.</p>
 */
@Controller
public class AuthController {

    private static final SecurityContextRepository CONTEXT_REPOSITORY =
            new HttpSessionSecurityContextRepository();

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/login")
    public String login(
            Authentication authentication,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            Model model
    ) {
        if (isSignedIn(authentication)) {
            return "redirect:/panel";
        }

        model.addAttribute("loginError", error != null);
        model.addAttribute("loggedOut", logout != null);
        return "auth/login";
    }

    @GetMapping("/registro")
    public String register(Authentication authentication, Model model) {
        if (isSignedIn(authentication)) {
            return "redirect:/panel";
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
            HttpServletRequest request,
            HttpServletResponse response
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

        // La cuenta queda creada y la persona entra directo a su panel:
        // no tiene que volver a escribir la contraseña que acaba de elegir.
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        form.getEmail().trim(),
                        form.getPassword()
                )
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        CONTEXT_REPOSITORY.saveContext(context, request, response);

        return "redirect:/panel?bienvenida";
    }

    private boolean isSignedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
