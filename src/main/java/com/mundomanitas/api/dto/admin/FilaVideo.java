package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Video con el nombre legible de su módulo y curso. */
public record FilaVideo(UUID id, String moduloTitulo, String cursoTitulo, String titulo, String url, int posicion) {
}
