package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.AccessibilityForm;
import com.mundomanitas.api.model.AccessibilityPreferences;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.AccessibilityService;
import com.mundomanitas.api.service.AccountService;
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
public class AccessibilityController {

    private static final List<String> SIZES = List.of("SMALL", "MEDIUM", "LARGE");
    private static final List<BigDecimal> SPEEDS = List.of(
            new BigDecimal("0.50"), new BigDecimal("0.75"), BigDecimal.ONE,
            new BigDecimal("1.25"), new BigDecimal("1.50"), new BigDecimal("2.00"));

    private final AccountService accounts;
    private final AccessibilityService accessibility;

    public AccessibilityController(AccountService accounts, AccessibilityService accessibility) {
        this.accounts = accounts;
        this.accessibility = accessibility;
    }

    @GetMapping
    public String show(
            @AuthenticationPrincipal AuthenticatedUser principal,
            Model model
    ) {
        User user = accounts.require(principal.getId());
        if (!model.containsAttribute("accessibilityForm")) {
            model.addAttribute("accessibilityForm", formFor(accessibility.forUser(user)));
        }
        return view(model);
    }

    @PostMapping
    public String save(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @ModelAttribute("accessibilityForm") AccessibilityForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirect
    ) {
        if (result.hasErrors()) {
            return view(model);
        }
        accessibility.save(accounts.require(principal.getId()), form);
        redirect.addFlashAttribute("panelOk", "Tus preferencias de accesibilidad se guardaron.");
        return "redirect:/panel/accesibilidad";
    }

    private String view(Model model) {
        model.addAttribute("captionSizes", SIZES);
        model.addAttribute("speeds", SPEEDS);
        return "student/accesibilidad";
    }

    private AccessibilityForm formFor(AccessibilityPreferences preferences) {
        AccessibilityForm form = new AccessibilityForm();
        form.setSignLanguageEnabled(preferences.isSignLanguageEnabled());
        form.setSignLanguageVariant(preferences.getSignLanguageVariant());
        form.setCaptionsEnabled(preferences.isCaptionsEnabled());
        form.setCaptionSize(preferences.getCaptionSize());
        form.setPlaybackSpeed(preferences.getPlaybackSpeed());
        return form;
    }
}
