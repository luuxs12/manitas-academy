package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.MonitorSnapshot;
import com.mundomanitas.api.service.admin.MonitorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Datos del monitor para el polling de la vista; solo para administradores. */
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/api/monitoreo")
public class AdminMonitoreoApiController {

    private final MonitorService monitor;

    public AdminMonitoreoApiController(MonitorService monitor) {
        this.monitor = monitor;
    }

    @GetMapping
    public MonitorSnapshot datos() {
        return monitor.snapshot();
    }
}
