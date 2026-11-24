package com.mundomanitas.api.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/** Almacenamiento local de la foto de perfil. */
public interface AvatarStorage {

    String store(MultipartFile file);

    void delete(String url);

    Path directory();
}
