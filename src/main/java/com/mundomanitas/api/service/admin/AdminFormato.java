package com.mundomanitas.api.service.admin;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/** Formatea fechas de la base para las tablas y los avisos del panel. */
@Component
public class AdminFormato {

    private static final DateTimeFormatter FULL =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Clock clock;

    public AdminFormato(Clock clock) {
        this.clock = clock;
    }

    public String completo(OffsetDateTime value) {
        return value == null ? "—" : value.atZoneSameInstant(clock.getZone()).format(FULL);
    }

    public String relative(OffsetDateTime value) {
        if (value == null) return "—";
        long minutes = Duration.between(value, OffsetDateTime.now(clock)).toMinutes();
        if (minutes < 1) return "ahora";
        if (minutes < 60) return "hace " + minutes + " min";
        long hours = minutes / 60;
        if (hours < 24) return "hace " + hours + " h";
        long days = hours / 24;
        if (days < 30) return "hace " + days + (days == 1 ? " día" : " días");
        long months = days / 30;
        return "hace " + months + (months == 1 ? " mes" : " meses");
    }

    public String initials(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) return "?";
        String[] parts = nombreCompleto.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (first + second).toUpperCase();
    }
}
