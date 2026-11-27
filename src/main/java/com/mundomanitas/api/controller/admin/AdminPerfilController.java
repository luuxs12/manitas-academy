package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CuentaService;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Cuenta del administrador: datos de acceso y foto del encabezado del panel. */
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/perfil")
public class AdminPerfilController extends AdminBaseController {

    private final CuentaService accounts;

    public AdminPerfilController(AdminNotificacionService notifications, CuentaService accounts) {
        super(notifications);
        this.accounts = accounts;
    }

    @GetMapping
    public String perfil(@AuthenticationPrincipal AutenticadoUsuario principal, Model model) {
        common(model, "perfil", "Mi perfil");
        model.addAttribute("perfil", accounts.require(actorId(principal)));
        return "admin/perfil";
    }

    @PostMapping("/foto")
    public String cargaFoto(@AuthenticationPrincipal AutenticadoUsuario principal,
                            @RequestParam("avatar") MultipartFile avatar,
                            RedirectAttributes redirect) {
        try {
            accounts.updateAvatar(accounts.require(actorId(principal)), avatar);
            redirect.addFlashAttribute("panelOk", "Tu foto de perfil se actualizó.");
        } catch (ResponseStatusException exception) {
            redirect.addFlashAttribute("panelError", exception.getReason());
        }
        return "redirect:/admin/perfil#foto";
    }

    @PostMapping("/foto/eliminar")
    public String quitaFoto(@AuthenticationPrincipal AutenticadoUsuario principal,
                            RedirectAttributes redirect) {
        accounts.removeAvatar(accounts.require(actorId(principal)));
        redirect.addFlashAttribute("panelOk", "Volviste a usar tus iniciales.");
        return "redirect:/admin/perfil#foto";
    }
}
