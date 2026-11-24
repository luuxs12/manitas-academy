package com.mundomanitas.api.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Convierte enlaces de YouTube en un ID de 11 caracteres y arma el embed.
 *
 * <p>Solo se aceptan dominios de YouTube: el ID se valida antes de usarse en la
 * plantilla para no inyectar direcciones arbitrarias en el reproductor.</p>
 */
public final class YouTubeLink {

    private static final Pattern ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");

    private YouTubeLink() {
    }

    /** Devuelve el ID normalizado, o {@code null} si el enlace no es de YouTube. */
    public static String id(String value) {
        if (value == null || value.isBlank()) return null;
        String candidate = value.trim();
        if (ID.matcher(candidate).matches()) return candidate;
        URI uri;
        try {
            uri = new URI(candidate);
        } catch (URISyntaxException exception) {
            return null;
        }
        String host = uri.getHost();
        if (host == null) return null;
        String domain = host.toLowerCase().replaceFirst("^(www|m)\\.", "");
        String path = uri.getPath();
        if (path == null) return null;
        String[] parts = path.split("/");
        String segment = parts.length > 1 ? parts[1] : null;
        String found = null;
        if (domain.equals("youtu.be")) {
            found = segment;
        } else if (domain.equals("youtube.com") || domain.equals("youtube-nocookie.com")) {
            if ("/watch".equals(path)) {
                found = queryValue(uri.getRawQuery(), "v");
            } else if (parts.length > 2 && ("embed".equals(segment) || "shorts".equals(segment))) {
                found = parts[2];
            }
        }
        return found != null && ID.matcher(found).matches() ? found : null;
    }

    /** Embed sin cookies para el ID indicado; {@code null} si no es válido. */
    public static String embedUrl(String youtubeId) {
        String id = id(youtubeId);
        return id == null ? null : "https://www.youtube-nocookie.com/embed/" + id;
    }

    private static String queryValue(String query, String key) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            if (separator > 0 && pair.substring(0, separator).equals(key)) {
                return pair.substring(separator + 1);
            }
        }
        return null;
    }
}
