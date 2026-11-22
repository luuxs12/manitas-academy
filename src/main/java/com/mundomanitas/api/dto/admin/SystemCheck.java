package com.mundomanitas.api.dto.admin;

/** Estado real de un componente del sistema. */
public record SystemCheck(String name, String detail, boolean ok) {
}
