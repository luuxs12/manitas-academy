package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;

import java.util.List;
import java.util.UUID;

/**
 * Reglas de inscripción y avance de los cursos de una cuenta.
 */
public interface InscripcionService {

    List<Inscripcion> mine(Usuario usuario);

    List<Inscripcion> finalizado(Usuario usuario);

    boolean isInscripto(UUID usuarioId, String slug);

    Inscripcion buscarMine(Usuario usuario, String slug);

    Inscripcion enroll(Usuario usuario, String slug);

    Inscripcion complete(Usuario usuario, String slug);

    Inscripcion toggleVideo(Usuario usuario, String slug, UUID videoId);

    Inscripcion next(Usuario usuario);
}
