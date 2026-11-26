package com.mundomanitas.api.dto.admin;

/** Evento reciente que alimenta las notificaciones del panel por polling. */
public record AdminEvento(String id, String kind, String titulo, String detail, String when) {
}
