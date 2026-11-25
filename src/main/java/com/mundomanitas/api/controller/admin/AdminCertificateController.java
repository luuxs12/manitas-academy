package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.AdminCertificateForm;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCertificatePreviewService;
import com.mundomanitas.api.service.admin.AdminCertificateService;
import com.mundomanitas.api.service.admin.AdminNotificationService;
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
public class AdminCertificateController extends AdminBaseController {

    private final AdminCertificateService certificates;
    private final AdminCertificatePreviewService preview;
    private final AdminCatalogService catalog;

    public AdminCertificateController(AdminNotificationService notifications,
                                      AdminCertificateService certificates,
                                      AdminCatalogService catalog,
                                      AdminCertificatePreviewService preview) {
        super(notifications);
        this.certificates = certificates;
        this.catalog = catalog;
        this.preview = preview;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String query,
                       @RequestParam(name = "usuario", required = false) UUID userId,
                       @RequestParam(name = "curso", required = false) UUID courseId,
                       @RequestParam(name = "incluirRevocados", defaultValue = "false") boolean revoked,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        common(model, "certificados", "Certificados");
        model.addAttribute("page", certificates.certificates(revoked, userId, courseId, query,
                PageRequest.of(Math.max(page, 0), 10)));
        model.addAttribute("query", query);
        model.addAttribute("userId", userId);
        model.addAttribute("courseId", courseId);
        model.addAttribute("includeRevoked", revoked);
        model.addAttribute("eligible", certificates.eligible());
        model.addAttribute("adminCertificateForm", new AdminCertificateForm(null, null));
        model.addAttribute("courseOptions", catalog.courseOptions());
        model.addAttribute("userOptions", catalog.userNames().entrySet().stream()
                .map(entry -> new com.mundomanitas.api.dto.admin.AdminOption(
                        entry.getKey(), entry.getValue()))
                .toList());
        return "admin/certificados";
    }

    @PostMapping("/emitir")
    public String issue(@Valid @ModelAttribute AdminCertificateForm form,
                        BindingResult result,
                        @AuthenticationPrincipal AuthenticatedUser principal,
                        RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("panelError", "Elegí una persona y un curso válidos.");
            return "redirect:/admin/certificados";
        }
        try {
            String code = certificates.issue(form, principal.getId());
            redirect.addFlashAttribute("panelOk", "Certificado " + code + " emitido.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/certificados";
    }

    @GetMapping("/{id}/ver")
    public String view(@PathVariable UUID id, Model model) {
        AdminCertificatePreviewService.Preview certificate = preview.preview(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe ese certificado."));
        common(model, "certificados", "Certificado");
        model.addAttribute("preview", certificate);
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
                         @AuthenticationPrincipal AuthenticatedUser principal,
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
