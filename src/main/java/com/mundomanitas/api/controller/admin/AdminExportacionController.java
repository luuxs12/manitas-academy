package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.ResultadoExportacion;
import com.mundomanitas.api.service.admin.AdminExportacionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Descargas reales de cada módulo en CSV, XLSX o PDF. */
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/exportar")
public class AdminExportacionController {

    private final AdminExportacionService exporter;

    public AdminExportacionController(AdminExportacionService exporter) {
        this.exporter = exporter;
    }

    @GetMapping("/{modulo}")
    public ResponseEntity<byte[]> exportacion(@PathVariable String modulo,
                                         @RequestParam(name = "formato", defaultValue = "csv") String format) {
        ResultadoExportacion resultado = exporter.exportacion(modulo, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resultado.nombreArchivo() + "\"")
                .contentType(MediaType.parseMediaType(resultado.contenidoTipo()))
                .body(resultado.bytes());
    }
}
