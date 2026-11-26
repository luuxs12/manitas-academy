package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.AdminCertificadoFormulario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCertificadoVistapreviaService;
import com.mundomanitas.api.service.admin.AdminCertificadoService;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/certificados")
public class AdminCertificadoController extends AdminBaseController {

    private final AdminCertificadoService certificates;
    private final AdminCertificadoVistapreviaService vistaprevia;
    private final AdminCatalogService catalog;

    public AdminCertificadoController(AdminNotificacionService notifications,
                                      AdminCertificadoService certificates,
                                      AdminCatalogService catalog,
                                      AdminCertificadoVistapreviaService vistaprevia) {
        super(notifications);
        this.certificates = certificates;
        this.catalog = catalog;
        this.vistaprevia = vistaprevia;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String query,
                       @RequestParam(name = "usuario", required = false) UUID usuarioId,
                       @RequestParam(name = "curso", required = false) UUID cursoId,
                       @RequestParam(name = "incluirRevocados", defaultValue = "false") boolean revocado,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        common(model, "certificados", "Certificados");
        model.addAttribute("page", certificates.certificates(revocado, usuarioId, cursoId, query,
                PageRequest.of(Math.max(page, 0), 10)));
        model.addAttribute("query", query);
        model.addAttribute("usuarioId", usuarioId);
        model.addAttribute("cursoId", cursoId);
        model.addAttribute("includeRevocado", revocado);
        model.addAttribute("elegible", certificates.elegible());
        model.addAttribute("adminCertificadoFormulario", new AdminCertificadoFormulario(null, null));
        model.addAttribute("cursoOptions", catalog.cursoOptions());
        model.addAttribute("usuarioOptions", catalog.usuarioNames().entrySet().stream()
                .map(entry -> new com.mundomanitas.api.dto.admin.AdminOpcion(
                        entry.getKey(), entry.getValue()))
                .toList());
        return "admin/certificados";
    }

    @PostMapping("/emitir")
    public String issue(@Valid @ModelAttribute AdminCertificadoFormulario formulario,
                        BindingResult resultado,
                        @AuthenticationPrincipal AutenticadoUsuario principal,
                        RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            redirect.addFlashAttribute("panelError", "Elegí una persona y un curso válidos.");
            return "redirect:/admin/certificados";
        }
        try {
            String code = certificates.issue(formulario, principal.getId());
            redirect.addFlashAttribute("panelOk", "Certificado " + code + " emitido.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/certificados";
    }

    @GetMapping("/{id}/ver")
    public String view(@PathVariable UUID id, Model model) {
        AdminCertificadoVistapreviaService.Vistaprevia certificado = vistaprevia.vistaprevia(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe ese certificado."));
        common(model, "certificados", "Certificado");
        model.addAttribute("vistaprevia", certificado);
        return "admin/certificado";
    }

    @GetMapping("/{id}/revocar")
    public String confirmRevoke(@PathVariable UUID id,
                                @RequestParam(name = "code", required = false) String code,
                                Model model,
                                RedirectAttributes redirect) {
        List<String> details = code == null || code.isBlank()
                ? List.of() : List.of("Código: " + code.trim());
        return confirmation(model, "certificados", "Revocar certificado",
                "La persona dejará de tener vigente este certificado.",
                "/admin/certificados/" + id + "/revocar", "Revocar certificado",
                "/admin/certificados", details);
    }

    @PostMapping("/{id}/revocar")
    public String revoke(@PathVariable UUID id,
                         @AuthenticationPrincipal AutenticadoUsuario principal,
                         RedirectAttributes redirect) {
        try {
            certificates.revoke(id, principal.getId());
            redirect.addFlashAttribute("panelOk", "El certificado se revocó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/certificados";
    }
}
