package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.OlvideContrasenaFormulario;
import com.mundomanitas.api.dto.RestablecerContrasenaFormulario;
import com.mundomanitas.api.exception.InvalidoRestablecerTokenException;
import com.mundomanitas.api.service.ContrasenaRestablecerService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Objects;

/**
 * Pantallas de recuperación de contraseña.
 *
 * <p>El pedido responde siempre lo mismo, exista o no la cuenta, para no
 * revelar correos registrados.</p>
 */
@Controller
public class ContrasenaRestablecerController {

    private final ContrasenaRestablecerService contrasenaRestablecerService;

    public ContrasenaRestablecerController(ContrasenaRestablecerService contrasenaRestablecerService) {
        this.contrasenaRestablecerService = contrasenaRestablecerService;
    }

    @GetMapping("/recuperar")
    public String olvide(
            @RequestParam(name = "enviado", required = false) String sent,
            Model model
    ) {
        if (!model.containsAttribute("formulario")) {
            model.addAttribute("formulario", new OlvideContrasenaFormulario());
        }
        model.addAttribute("sent", sent != null);
        return "auth/forgot-password";
    }

    @PostMapping("/recuperar")
    public String olvide(
            @Valid @ModelAttribute("formulario") OlvideContrasenaFormulario formulario,
            BindingResult resultado
    ) {
        if (resultado.hasErrors()) {
            return "auth/forgot-password";
        }

        contrasenaRestablecerService.requestRestablecer(formulario.getEmail());
        return "redirect:/recuperar?enviado";
    }

    @GetMapping("/restablecer")
    public String restablecer(
            @RequestParam(name = "token", required = false) String token,
            Model model
    ) {
        boolean usable = contrasenaRestablecerService.isUsable(token);

        if (!usable) {
            model.addAttribute("invalido", true);
            return "auth/reset-password";
        }

        if (!model.containsAttribute("formulario")) {
            RestablecerContrasenaFormulario formulario = new RestablecerContrasenaFormulario();
            formulario.setToken(token);
            model.addAttribute("formulario", formulario);
        }
        model.addAttribute("invalido", false);
        return "auth/reset-password";
    }

    @PostMapping("/restablecer")
    public String restablecer(
            @Valid @ModelAttribute("formulario") RestablecerContrasenaFormulario formulario,
            BindingResult resultado,
            Model model
    ) {
        if (!Objects.equals(formulario.getPassword(), formulario.getConfirmContrasena())) {
            resultado.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Las contraseñas no coinciden."
            );
        }

        if (resultado.hasErrors()) {
            model.addAttribute("invalido", false);
            return "auth/reset-password";
        }

        try {
            contrasenaRestablecerService.restablecerContrasena(formulario.getToken(), formulario.getPassword());
        } catch (InvalidoRestablecerTokenException exception) {
            model.addAttribute("invalido", true);
            return "auth/reset-password";
        }

        return "redirect:/?login&reset";
    }
}
