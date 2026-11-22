package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Video con el nombre legible de su módulo y curso. */
public record VideoRow(UUID id, String moduleTitle, String courseTitle, String title, String url, int position) {
}
