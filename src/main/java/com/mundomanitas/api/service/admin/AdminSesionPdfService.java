package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.ResultadoExportacion;

import java.util.UUID;

/** PDF del temario real de una sesión (módulo) de un curso. */
public interface AdminSesionPdfService {

    ResultadoExportacion moduloPdf(UUID moduloId, UUID actor);
}
