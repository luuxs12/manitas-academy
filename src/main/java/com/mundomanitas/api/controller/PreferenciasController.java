package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.CuentaDeleteFormulario;
import com.mundomanitas.api.dto.PreferenciasFormulario;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CuentaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Preferencias de la cuenta y cierre definitivo de la misma. */
@Controller
@RequestMapping("/panel/preferencias")
public class PreferenciasController {

    private static final String[] THEMES = {"SYSTEM", "LIGHT", "DARK"};

    private final CuentaService accounts;

    public PreferenciasController(CuentaService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public String show(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            Model model
    ) {
        Usuario usuario = accounts.require(principal.getId());
        if (!model.containsAttribute("preferenciasFormulario")) {
            PreferenciasFormulario formulario = new PreferenciasFormulario();
            formulario.setTheme(usuario.getTheme() == null ? "SYSTEM" : usuario.getTheme());
            formulario.setNotifyEmail(usuario.isNotifyEmail());
            model.addAttribute("preferenciasFormulario", formulario);
        }
        if (!model.containsAttribute("deleteFormulario")) {
            model.addAttribute("deleteFormulario", new CuentaDeleteFormulario());
        }
        model.addAttribute("themes", THEMES);
        return "student/preferencias";
    }

    @PostMapping
    public String save(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @ModelAttribute("preferenciasFormulario") PreferenciasFormulario formulario,
            RedirectAttributes redirect
    ) {
        accounts.savePreferencias(accounts.require(principal.getId()), formulario);
        redirect.addFlashAttribute("panelOk", "Tus preferencias se guardaron.");
        return "redirect:/panel/preferencias";
    }

    @PostMapping("/eliminar")
    public String delete(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @ModelAttribute("deleteFormulario") CuentaDeleteFormulario formulario,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirect
    ) {
        Usuario usuario = accounts.require(principal.getId());
        if (!accounts.deleteCuenta(usuario, formulario)) {
            redirect.addFlashAttribute(
                    "panelError", "Escribí ELIMINAR para confirmar el cierre de la cuenta.");
            return "redirect:/panel/preferencias";
        }
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/?login";
    }
}
