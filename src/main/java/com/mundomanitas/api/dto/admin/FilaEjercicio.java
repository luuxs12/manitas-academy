package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Ejercicio con el nombre legible de su módulo y curso. */
public record FilaEjercicio(UUID id, String moduloTitulo, String cursoTitulo, String titulo, String prompt, int posicion) {
}
