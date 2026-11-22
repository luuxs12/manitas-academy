package com.mundomanitas.api.controller;

import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.AccountService;
import com.mundomanitas.api.service.CourseCatalog;
import com.mundomanitas.api.service.EnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final AccountService accounts;
    private final EnrollmentService enrollments;

    public CourseController(
            CourseCatalog catalog,
            AccountService accounts,
            EnrollmentService enrollments
    ) {
        this.catalog = catalog;
        this.accounts = accounts;
        this.enrollments = enrollments;
    }

    @GetMapping("/cursos")
    public String catalog(Model model) {
        model.addAttribute("courses", catalog.all());
        model.addAttribute("areas", catalog.categories());
        model.addAttribute("query", "");
        return "cursos/index";
    }

    @GetMapping("/buscar")
    public String search(
            @RequestParam(name = "q", defaultValue = "") String query,
            Model model
    ) {
        model.addAttribute("courses", catalog.search(query));
        model.addAttribute("areas", catalog.categories());
        model.addAttribute("query", query);
        return "cursos/index";
    }

    @GetMapping("/cursos/{slug}")
    public String detail(
            @PathVariable String slug,
            @AuthenticationPrincipal AuthenticatedUser principal,
            Model model
    ) {
        Course course = catalog.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Curso no encontrado"));

        boolean signedIn = principal != null;
        boolean premium = signedIn && accounts.require(principal.getId()).isPremium();
        boolean enrolled = signedIn && enrollments.isEnrolled(principal.getId(), slug);

        model.addAttribute("course", course);
        model.addAttribute("related", catalog.others(course, 3));
        model.addAttribute("signedIn", signedIn);
        model.addAttribute("enrolled", enrolled);
        model.addAttribute("canEnroll", signedIn && (course.isFree() || premium));
        return "cursos/detalle";
    }
}
