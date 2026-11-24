package com.mundomanitas.api.service;

import org.springframework.web.multipart.MultipartFile;

/** Guarda la portada de un curso en disco y devuelve su ruta pública. */
public interface CourseCoverStorage {

    String store(MultipartFile file);

    void delete(String url);

    java.nio.file.Path directory();
}
