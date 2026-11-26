package com.mundomanitas.api.dto.admin;

/** Tarjeta de indicador: el valor y la tendencia salen de PostgreSQL. */
public record TarjetaKpi(String label, String value, String trend, String iconPath, String detail) {
}
