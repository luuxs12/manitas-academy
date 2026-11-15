package com.mundomanitas.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Páginas legales enlazadas desde el pie: solo texto, sin modelo.
 */
@Controller
public class LegalController {

    @GetMapping("/terminos")
    public String terms() {
        return "legal/terminos";
    }

    @GetMapping("/privacidad")
    public String privacy() {
        return "legal/privacidad";
    }
}
