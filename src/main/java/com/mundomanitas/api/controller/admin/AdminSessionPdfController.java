package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.ExportResult;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.admin.AdminSessionPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Descarga del PDF de una sesión real desde el temario administrativo. */
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/exportar/sesion")
public class AdminSessionPdfController {

    private final AdminSessionPdfService sessions;

    public AdminSessionPdfController(AdminSessionPdfService sessions) {
        this.sessions = sessions;
    }

    @GetMapping("/modulos/{moduleId}/pdf")
    public ResponseEntity<byte[]> modulePdf(@PathVariable UUID moduleId,
                                            @AuthenticationPrincipal AuthenticatedUser principal) {
        ExportResult result = sessions.modulePdf(moduleId, principal.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.bytes());
    }
}
