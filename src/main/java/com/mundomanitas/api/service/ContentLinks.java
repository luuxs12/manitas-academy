package com.mundomanitas.api.service;

import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkArgument;

/** Validación de los únicos enlaces que acepta el contenido del curso. */
public final class ContentLinks {

    private ContentLinks() {
    }

    /** ID de YouTube de 11 caracteres, extraído solo de dominios de YouTube. */
    public static String youtubeId(String value) {
        String id = YouTubeLink.id(value);
        checkArgument(id != null, "El enlace de YouTube no es válido");
        return id;
    }

    /** Enlace externo seguro: solo https con host explícito. */
    public static String https(String value, String field) {
        checkArgument(value != null && !value.isBlank(), "El campo " + field + " es obligatorio");
        try {
            URI uri = new URI(value.trim());
            checkArgument("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null,
                    "El enlace debe usar https");
            return uri.toString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("El enlace debe usar https", exception);
        }
    }
}
