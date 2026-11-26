package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.ResultadoExportacion;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.admin.AdminSesionPdfService;
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
public class AdminSesionPdfController {

    private final AdminSesionPdfService sessions;

    public AdminSesionPdfController(AdminSesionPdfService sessions) {
        this.sessions = sessions;
    }

    @GetMapping("/modulos/{moduloId}/pdf")
    public ResponseEntity<byte[]> moduloPdf(@PathVariable UUID moduloId,
                                            @AuthenticationPrincipal AutenticadoUsuario principal) {
        ResultadoExportacion resultado = sessions.moduloPdf(moduloId, principal.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resultado.nombreArchivo() + "\"")
                .contentType(MediaType.parseMediaType(resultado.contenidoTipo()))
                .body(resultado.bytes());
    }
}
