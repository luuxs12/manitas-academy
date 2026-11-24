package com.mundomanitas.api.controller;

import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.service.CourseCatalog;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

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
        List<Course> published = catalog.all();
        model.addAttribute("courses", published);
        model.addAttribute("featured", published.stream()
                .filter(Course::isFree)
                .findFirst()
                .orElse(null));
        return "index";
    }

    @GetMapping("/nosotros")
    public String about() {
        return "nosotros";
    }
}
