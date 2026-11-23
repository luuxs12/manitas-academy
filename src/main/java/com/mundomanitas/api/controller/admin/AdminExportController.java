package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.ExportResult;
import com.mundomanitas.api.service.admin.AdminExportService;
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
public class AdminExportController {

    private final AdminExportService exporter;

    public AdminExportController(AdminExportService exporter) {
        this.exporter = exporter;
    }

    @GetMapping("/{module}")
    public ResponseEntity<byte[]> export(@PathVariable String module,
                                         @RequestParam(name = "formato", defaultValue = "csv") String format) {
        ExportResult result = exporter.export(module, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.bytes());
    }
}
