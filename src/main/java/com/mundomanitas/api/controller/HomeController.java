package com.mundomanitas.api.controller;

import com.mundomanitas.api.service.CourseCatalog;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página principal de la plataforma.
 */
@Controller
public class HomeController {

    private final CourseCatalog catalog;

    public HomeController(CourseCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("courses", catalog.all());
        return "index";
    }

    @GetMapping("/nosotros")
    public String about() {
        return "nosotros";
    }
}
