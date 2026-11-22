package com.mundomanitas.api.dto.admin;

/** Evento reciente que alimenta las notificaciones del panel por polling. */
public record AdminEvent(String id, String kind, String title, String detail, String when) {
}
