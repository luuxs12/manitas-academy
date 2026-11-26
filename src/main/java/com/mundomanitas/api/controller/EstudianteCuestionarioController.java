package com.mundomanitas.api.controller;

import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CuentaService;
import com.mundomanitas.api.service.CuestionarioService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cuestionario definitivo de una práctica del temario del alumno. */
@Controller
@RequestMapping("/panel/cursos/{slug}/ejercicios")
@PreAuthorize("hasRole('STUDENT')")
public class EstudianteCuestionarioController {

    private final CuentaService accounts;
    private final CuestionarioService quizzes;

    public EstudianteCuestionarioController(CuentaService accounts, CuestionarioService quizzes) {
        this.accounts = accounts;
        this.quizzes = quizzes;
    }

    @GetMapping("/{itemId}")
    @PreAuthorize("hasRole('STUDENT')")
    public String show(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @PathVariable String slug,
            @PathVariable UUID itemId,
            Model model
    ) {
        model.addAttribute("cuestionario", quizzes.view(accounts.require(principal.getId()), slug, itemId));
        model.addAttribute("slug", slug);
        model.addAttribute("itemId", itemId);
        return "student/ejercicio";
    }

    @PostMapping("/{itemId}/enviar")
    @PreAuthorize("hasRole('STUDENT')")
    public String submit(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @PathVariable String slug,
            @PathVariable UUID itemId,
            @RequestParam Map<String, String> parameters,
            RedirectAttributes redirect
    ) {
        String path = "/panel/cursos/" + slug + "/ejercicios/" + itemId;
        try {
            CuestionarioService.IntentoVista resultado = quizzes.submit(accounts.require(principal.getId()),
                    slug, itemId, responses(parameters));
            redirect.addFlashAttribute("panelOk",
                    "Resultado guardado: " + resultado.score() + " / 20.");
        } catch (ResponseStatusException exception) {
            redirect.addFlashAttribute("panelError", exception.getReason() == null
                    ? "No se pudo guardar el resultado." : exception.getReason());
        }
        return "redirect:" + path;
    }

    /** Convierte `respuestas[id-pregunta]` en el mapa que corrige el servicio. */
    private Map<UUID, UUID> responses(Map<String, String> parameters) {
        Map<UUID, UUID> resultado = new HashMap<>();
        parameters.forEach((name, value) -> {
            if (name != null && name.startsWith("respuestas[") && name.endsWith("]")) {
                try {
                    resultado.put(UUID.fromString(name.substring("respuestas[".length(),
                                    name.length() - 1)),
                            UUID.fromString(value));
                } catch (IllegalArgumentException exception) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Las respuestas enviadas no son válidas.");
                }
            }
        });
        return resultado;
    }
}
