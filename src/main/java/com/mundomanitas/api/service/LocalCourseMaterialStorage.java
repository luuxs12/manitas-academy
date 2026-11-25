package com.mundomanitas.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Documentos en disco local; la descarga pasa siempre por el panel del alumno. */
@Service
public class LocalCourseMaterialStorage implements CourseMaterialStorage {

    private static final Map<String, String> BY_CONTENT_TYPE = Map.of(
            "application/pdf", "pdf",
            "application/msword", "doc",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx",
            "application/vnd.ms-powerpoint", "ppt",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx",
            "text/plain", "txt"
    );

    private static final Map<String, String> BY_EXTENSION = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "ppt", "application/vnd.ms-powerpoint",
            "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "txt", "text/plain"
    );

    private final Path directory;
    private final long maxBytes;

    public LocalCourseMaterialStorage(
            @Value("${app.upload.materials-dir:./uploads/materials}") String directory,
            @Value("${app.upload.max-material-bytes:10485760}") long maxBytes
    ) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elegí un documento");
        }
        String extension = extensionOf(file);
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe ser PDF, DOC, DOCX, PPT, PPTX o TXT");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento no puede superar el tamaño permitido");
        }
        String name = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(name));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar el documento", exception);
        }
        return URL_PREFIX + name;
    }

    @Override
    public byte[] read(String url) {
        Path file = resolve(url);
        try {
            return Files.readAllBytes(file);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El documento no está disponible", exception);
        }
    }

    @Override
    public String contentType(String url) {
        String name = fileNameOf(url);
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return BY_EXTENSION.getOrDefault(extension, "application/octet-stream");
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) return;
        String name = fileNameOf(url);
        try {
            Files.deleteIfExists(directory.resolve(name));
        } catch (IOException exception) {
            // La base ya no apunta al archivo: no frenamos el borrado del ítem.
        }
    }

    @Override
    public Path directory() {
        return directory;
    }

    private Path resolve(String url) {
        String name = fileNameOf(url);
        Path file = directory.resolve(name).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El documento no existe");
        }
        return file;
    }

    private String fileNameOf(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El documento no existe");
        }
        String name = url.substring(URL_PREFIX.length());
        if (!name.matches("[0-9a-f-]{36}\\.(pdf|doc|docx|ppt|pptx|txt)")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El documento no existe");
        }
        return name;
    }

    private String extensionOf(MultipartFile file) {
        String fromType = BY_CONTENT_TYPE.get(file.getContentType());
        if (fromType != null) return fromType;
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) return null;
        String base = Path.of(original.replace('\\', '/')).getFileName().toString();
        int dot = base.lastIndexOf('.');
        if (dot < 0) return null;
        String extension = base.substring(dot + 1).toLowerCase(Locale.ROOT);
        return BY_EXTENSION.containsKey(extension) ? extension : null;
    }
}
