package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminQuizForm;

import java.util.UUID;

/** Alta y edición administrativa del cuestionario de un ejercicio. */
public interface AdminQuizService {

    /** Devuelve el cuestionario guardado o diez preguntas vacías para cargarlo. */
    AdminQuizForm form(UUID exerciseId);

    /** Guarda o reemplaza el cuestionario; no se permite si ya hay intentos. */
    void save(UUID exerciseId, AdminQuizForm form, UUID actor);
}
