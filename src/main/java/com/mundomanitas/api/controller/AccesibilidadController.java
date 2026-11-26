package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.AccesibilidadFormulario;
import com.mundomanitas.api.model.AccesibilidadPreferencias;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.AccesibilidadService;
import com.mundomanitas.api.service.CuentaService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/** Preferencias reales de LSP, subtítulos y velocidad. */
@Controller
@RequestMapping("/panel/accesibilidad")
public class AccesibilidadController {

    private static final List<String> SIZES = List.of("SMALL", "MEDIUM", "LARGE");
    private static final List<BigDecimal> SPEEDS = List.of(
            new BigDecimal("0.50"), new BigDecimal("0.75"), BigDecimal.ONE,
            new BigDecimal("1.25"), new BigDecimal("1.50"), new BigDecimal("2.00"));

    private final CuentaService accounts;
    private final AccesibilidadService accesibilidad;

    public AccesibilidadController(CuentaService accounts, AccesibilidadService accesibilidad) {
        this.accounts = accounts;
        this.accesibilidad = accesibilidad;
    }

    @GetMapping
    public String show(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            Model model
    ) {
        Usuario usuario = accounts.require(principal.getId());
        if (!model.containsAttribute("accesibilidadFormulario")) {
            model.addAttribute("accesibilidadFormulario", formularioFor(accesibilidad.forUsuario(usuario)));
        }
        return view(model);
    }

    @PostMapping
    public String save(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @Valid @ModelAttribute("accesibilidadFormulario") AccesibilidadFormulario formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect
    ) {
        if (resultado.hasErrors()) {
            return view(model);
        }
        accesibilidad.save(accounts.require(principal.getId()), formulario);
        redirect.addFlashAttribute("panelOk", "Tus preferencias de accesibilidad se guardaron.");
        return "redirect:/panel/accesibilidad";
    }

    private String view(Model model) {
        model.addAttribute("captionSizes", SIZES);
        model.addAttribute("speeds", SPEEDS);
        return "student/accesibilidad";
    }

    private AccesibilidadFormulario formularioFor(AccesibilidadPreferencias preferencias) {
        AccesibilidadFormulario formulario = new AccesibilidadFormulario();
        formulario.setSignLanguageEnabled(preferencias.isSignLanguageEnabled());
        formulario.setSignLanguageVariant(preferencias.getSignLanguageVariant());
        formulario.setCaptionsEnabled(preferencias.isCaptionsEnabled());
        formulario.setCaptionSize(preferencias.getCaptionSize());
        formulario.setReproduccionSpeed(preferencias.getReproduccionSpeed());
        return formulario;
    }
}
