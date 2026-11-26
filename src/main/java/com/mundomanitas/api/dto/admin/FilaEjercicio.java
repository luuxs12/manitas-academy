package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Ejercicio con el nombre legible de su módulo, curso y estado del cuestionario. */
public record FilaEjercicio(UUID id, String moduloTitulo, String cursoTitulo, String titulo,
                           String prompt, int posicion, int preguntas, boolean marcarAlRendir) {

    /** Conserva la firma anterior para vistas que no muestran el estado. */
    public FilaEjercicio(UUID id, String moduloTitulo, String cursoTitulo, String titulo,
                         String prompt, int posicion) {
        this(id, moduloTitulo, cursoTitulo, titulo, prompt, posicion, 0, true);
    }
}
