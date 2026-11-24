package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Alta y edición de un video.
 *
 * <p>El enlace de YouTube es el origen de los videos nuevos. La URL y las
 * claves de R2 quedan como respaldo de los videos históricos.</p>
 */
public record AdminVideoForm(
        @NotNull(message = "Elegí un módulo.") UUID moduleId,
        @NotBlank(message = "Ingresá el título del video.") String title,
        @Size(max = 300, message = "La URL del video no puede superar los 300 caracteres.")
        @Pattern(regexp = "^$|^https?://.+$|^/video/[A-Za-z0-9/_-]+\\.mp4$",
                message = "Usá una URL http(s) o una ruta /video/… real del proyecto.")
        String url,
        @Min(value = 0, message = "La posición no puede ser negativa.") int position,
        @Pattern(regexp = "^$|^(?!.*\\.\\.)videos/[A-Za-z0-9_./-]+\\.(mp4|webm|mov)$",
                message = "La clave del video no es válida.")
        String objectKey,
        @Pattern(regexp = "^$|^(?!.*\\.\\.)subtitles/[A-Za-z0-9_./-]+\\.vtt$",
                message = "La clave del subtítulo no es válida.")
        String subtitleKey,
        @NotNull(message = "Ingresá la duración del video.")
        @Min(value = 0, message = "La duración no puede ser negativa.")
        @Max(value = 86400, message = "La duración no puede superar un día.")
        Integer durationSeconds,
        @Size(max = 2000, message = "La transcripción no puede superar los 2000 caracteres.")
        String transcript,
        @Size(max = 300, message = "El enlace de YouTube no puede superar los 300 caracteres.")
        @Pattern(regexp = "^$|^[A-Za-z0-9_-]{11}$"
                        + "|^https?://(?:www\\.)?(?:youtube\\.com|youtu\\.be|youtube-nocookie\\.com)/.+$",
                message = "Pegá un enlace válido de YouTube.")
        String youtubeUrl
) {
    /** Firma anterior, conservada para los videos históricos sin YouTube. */
    public AdminVideoForm(UUID moduleId, String title, String url, int position,
                          String objectKey, String subtitleKey, Integer durationSeconds,
                          String transcript) {
        this(moduleId, title, url, position, objectKey, subtitleKey,
                durationSeconds, transcript, null);
    }

    /** Normaliza los campos opcionales para no guardar cadenas vacías. */
    public static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
