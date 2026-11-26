package com.mundomanitas.api.dto.admin;

/** Estado real de un componente del sistema. */
public record VerificacionSistema(String name, String detail, boolean ok) {
}
