package com.mundomanitas.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/** Guarda la foto de perfil en disco; la ruta se escribe en PostgreSQL. */
@Service
public class LocalAvatarStorage implements AvatarStorage {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private static final String URL_PREFIX = "/uploads/avatars/";

    private final Path directory;
    private final long maxBytes;

    public LocalAvatarStorage(
            @Value("${app.upload.avatars-dir:./uploads/avatars}") String directory,
            @Value("${app.upload.max-avatar-bytes:2097152}") long maxBytes
    ) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elegí una imagen");
        }
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La foto debe ser JPG, PNG o WebP");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La foto no puede superar los 2 MB");
        }
        String name = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(name));
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la foto", exception);
        }
        return URL_PREFIX + name;
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) return;
        String name = url.substring(URL_PREFIX.length());
        if (!name.matches("[0-9a-f-]+\\.(jpg|png|webp)")) return;
        try {
            Files.deleteIfExists(directory.resolve(name));
        } catch (IOException exception) {
            // La base ya no apunta al archivo: no frenamos el cambio de foto.
        }
    }

    @Override
    public Path directory() {
        return directory;
    }
}
