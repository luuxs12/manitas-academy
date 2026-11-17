package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.CurrentUser;
import com.mundomanitas.api.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expone la persona autenticada a todas las vistas para que la cabecera
 * pueda mostrar su nombre y el botón de salir.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentUser")
    public CurrentUser currentUser(Authentication authentication) {
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return new CurrentUser(user.getFullName(), user.getEmail());
        }
        return null;
    }

    /**
     * Resuelve el token CSRF antes de escribir la respuesta. El modal de
     * cuenta vive al final de páginas largas: si el token se pidiera recién
     * al renderizar sus formularios, la respuesta ya estaría enviada y no se
     * podría abrir la sesión que guarda el token.
     */
    @ModelAttribute
    public void materializeCsrfToken(HttpServletRequest request) {
        if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
            token.getToken();
        }
    }
}
