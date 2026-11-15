package com.mundomanitas.api.controller;

import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.service.CourseCatalog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Catálogo público y búsqueda con {@code GET}.
 */
@Controller
public class CourseController {

    private final CourseCatalog catalog;

    public CourseController(CourseCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/cursos")
    public String catalog(Model model) {
        model.addAttribute("courses", catalog.all());
        model.addAttribute("query", "");
        return "cursos/index";
    }

    @GetMapping("/buscar")
    public String search(
            @RequestParam(name = "q", defaultValue = "") String query,
            Model model
    ) {
        model.addAttribute("courses", catalog.search(query));
        model.addAttribute("query", query);
        return "cursos/index";
    }

    @GetMapping("/cursos/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        Course course = catalog.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Curso no encontrado"));
        model.addAttribute("course", course);
        return "cursos/detalle";
    }
}
