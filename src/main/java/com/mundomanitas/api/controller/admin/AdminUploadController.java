package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.service.R2StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.function.Supplier;

/** Sube imágenes y videos a R2 desde los formularios del panel admin. */
@RestController
@RequestMapping("/admin/archivos")
public class AdminUploadController {

    private final R2StorageService storage;

    public AdminUploadController(R2StorageService storage) {
        this.storage = storage;
    }

    @PostMapping(value = "/imagenes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> image(@RequestParam("archivo") MultipartFile file) {
        return store(() -> storage.storeImage(file));
    }

    @PostMapping(value = "/videos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> video(@RequestParam("archivo") MultipartFile file) {
        return store(() -> storage.storeVideo(file));
    }

    private ResponseEntity<?> store(Supplier<String> upload) {
        try {
            return ResponseEntity.ok(Map.of(
                    "url", upload.get(),
                    "provider", "r2"
            ));
        } catch (ResponseStatusException exception) {
            String reason = exception.getReason() == null
                    ? "No se pudo subir el archivo."
                    : exception.getReason();
            return ResponseEntity.status(exception.getStatusCode())
                    .body(Map.of("error", reason));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No se pudo subir el archivo."));
        }
    }
}
