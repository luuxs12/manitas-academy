package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.CurrentUser;
import com.mundomanitas.api.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
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
}
