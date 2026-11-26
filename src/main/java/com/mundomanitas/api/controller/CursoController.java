package com.mundomanitas.api.controller;

import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CuentaService;
import com.mundomanitas.api.service.CursoCatalog;
import com.mundomanitas.api.service.InscripcionService;
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
public class CursoController {

    private final CursoCatalog catalog;
    private final CuentaService accounts;
    private final InscripcionService enrollments;

    public CursoController(
            CursoCatalog catalog,
            CuentaService accounts,
            InscripcionService enrollments
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
            @AuthenticationPrincipal AutenticadoUsuario principal,
            Model model
    ) {
        Curso curso = catalog.buscarPorSlug(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Curso no encontrado"));

        boolean signedIn = principal != null;
        boolean premium = signedIn && accounts.require(principal.getId()).isPremium();
        boolean inscripto = signedIn && enrollments.isInscripto(principal.getId(), slug);

        model.addAttribute("curso", curso);
        model.addAttribute("related", catalog.others(curso, 3));
        model.addAttribute("signedIn", signedIn);
        model.addAttribute("inscripto", inscripto);
        model.addAttribute("canEnroll", signedIn && (curso.isFree() || premium));
        return "cursos/detalle";
    }
}
