package com.mundomanitas.api.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/** Guarda y sirve documentos del curso desde una carpeta privada del servidor. */
public interface CursoMaterialStorage {

    String URL_PREFIX = "/uploads/materials/";

    /** Guarda el archivo y devuelve su ruta interna, nunca el nombre original. */
    String store(MultipartFile file);

    byte[] read(String url);

    String contenidoTipo(String url);

    void delete(String url);

    Path directory();
}
