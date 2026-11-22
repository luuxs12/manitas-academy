package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.AccountDeleteForm;
import com.mundomanitas.api.dto.PreferencesForm;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.AccountService;
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
public class PreferencesController {

    private static final String[] THEMES = {"SYSTEM", "LIGHT", "DARK"};

    private final AccountService accounts;

    public PreferencesController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public String show(
            @AuthenticationPrincipal AuthenticatedUser principal,
            Model model
    ) {
        User user = accounts.require(principal.getId());
        if (!model.containsAttribute("preferencesForm")) {
            PreferencesForm form = new PreferencesForm();
            form.setTheme(user.getTheme() == null ? "SYSTEM" : user.getTheme());
            form.setNotifyEmail(user.isNotifyEmail());
            model.addAttribute("preferencesForm", form);
        }
        if (!model.containsAttribute("deleteForm")) {
            model.addAttribute("deleteForm", new AccountDeleteForm());
        }
        model.addAttribute("themes", THEMES);
        return "student/preferencias";
    }

    @PostMapping
    public String save(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @ModelAttribute("preferencesForm") PreferencesForm form,
            RedirectAttributes redirect
    ) {
        accounts.savePreferences(accounts.require(principal.getId()), form);
        redirect.addFlashAttribute("panelOk", "Tus preferencias se guardaron.");
        return "redirect:/panel/preferencias";
    }

    @PostMapping("/eliminar")
    public String delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @ModelAttribute("deleteForm") AccountDeleteForm form,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirect
    ) {
        User user = accounts.require(principal.getId());
        if (!accounts.deleteAccount(user, form)) {
            redirect.addFlashAttribute(
                    "panelError", "Escribí ELIMINAR para confirmar el cierre de la cuenta.");
            return "redirect:/panel/preferencias";
        }
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/?login";
    }
}
