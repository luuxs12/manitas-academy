package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.RegistroFormulario;
import com.mundomanitas.api.exception.DuplicadoEmailException;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.service.AuthService;
import com.mundomanitas.api.service.CorreoService;
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

import java.util.Objects;

/**
 * Pantallas y acciones de acceso.
 *
 * <p>El acceso se abre como modal sobre el sitio; el POST de login lo atiende
 * Spring Security y acá solo se gestiona el alta de cuentas.</p>
 */
@Controller
public class AuthController {

    private static final SecurityContextRepository CONTEXT_REPOSITORY =
            new HttpSessionSecurityContextRepository();

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final CorreoService correoService;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            CorreoService correoService
    ) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.correoService = correoService;
    }

    /** La URL histórica no muestra pantalla: vuelve al inicio o al panel del rol. */
    @GetMapping("/login")
    public String legacyLogin(Authentication authentication) {
        return isSignedIn(authentication) ? panelFor(authentication) : "redirect:/?login";
    }

    @GetMapping("/registro")
    public String registro(Authentication authentication, Model model) {
        if (isSignedIn(authentication)) {
            return panelFor(authentication);
        }

        if (!model.containsAttribute("formulario")) {
            model.addAttribute("formulario", new RegistroFormulario());
        }
        return "auth/register";
    }

    @PostMapping("/registro")
    public String registro(
            @Valid @ModelAttribute("formulario") RegistroFormulario formulario,
            BindingResult resultado,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (!Objects.equals(formulario.getContrasena(), formulario.getConfirmContrasena())) {
            resultado.rejectValue(
                    "confirmContrasena",
                    "password.mismatch",
                    "Las contraseñas no coinciden."
            );
        }

        if (resultado.hasErrors()) {
            return "auth/register";
        }

        Usuario usuario;
        try {
            usuario = authService.registro(formulario);
        } catch (DuplicadoEmailException | DataIntegrityViolationException exception) {
            resultado.rejectValue(
                    "email",
                    "email.duplicate",
                    "Ya existe una cuenta con ese correo."
            );
            return "auth/register";
        }

        // El aviso de bienvenida es opcional: si Resend falla, la cuenta y la
        // sesión siguen adelante.
        correoService.sendWelcome(usuario);

        // La cuenta queda creada y la persona entra directo a su panel:
        // no tiene que volver a escribir la contraseña que acaba de elegir.
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        formulario.getEmail().trim(),
                        formulario.getContrasena()
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

    private String panelFor(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return admin ? "redirect:/admin" : "redirect:/panel";
    }
}
