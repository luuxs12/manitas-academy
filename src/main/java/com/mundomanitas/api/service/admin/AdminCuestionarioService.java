package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCuestionarioFormulario;

import java.util.UUID;

/** Alta y edición administrativa del cuestionario de un ejercicio. */
public interface AdminCuestionarioService {

    /** Devuelve el cuestionario guardado o diez preguntas vacías para cargarlo. */
    AdminCuestionarioFormulario formulario(UUID ejercicioId);

    /** Guarda o reemplaza el cuestionario; no se permite si ya hay intentos. */
    void save(UUID ejercicioId, AdminCuestionarioFormulario formulario, UUID actor);
}
