package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.ExportResult;

import java.util.UUID;

/** PDF del temario real de una sesión (módulo) de un curso. */
public interface AdminSessionPdfService {

    ExportResult modulePdf(UUID moduleId, UUID actor);
}
