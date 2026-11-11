package com.mundomanitas.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pantallas de acceso. Por ahora solo muestran la interfaz;
 * la lógica de registro y login se agrega en la siguiente fase.
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/registro")
    public String register() {
        return "auth/register";
    }
}
