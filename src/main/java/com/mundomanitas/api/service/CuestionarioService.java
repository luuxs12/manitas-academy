package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Usuario;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cuestionario de un ejercicio: se aprueba con 20/20 y se puede repetir si no se llega. */
public interface CuestionarioService {

    record AlternativaVista(UUID id, String label) {
    }

    record PreguntaVista(UUID id, int posicion, String prompt,
                        List<AlternativaVista> alternatives) {
    }

    record RespuestaVista(int posicion, String prompt, String selectedLabel, String correctLabel,
                      int pointsEarned) {
    }

    record IntentoVista(int score, OffsetDateTime submittedAt, List<RespuestaVista> answers) {
    }

    record CuestionarioVista(UUID itemId, String titulo, String prompt, boolean ready, boolean submitted,
                    boolean aprobado, IntentoVista intento, List<PreguntaVista> questions) {
    }

    /** Devuelve el cuestionario, el último resultado y si ya se alcanzó 20/20. */
    CuestionarioVista view(Usuario usuario, String slug, UUID itemId);

    /** Corrige y guarda el intento; 20/20 cierra el ejercicio, un puntaje menor se puede repetir. */
    IntentoVista submit(Usuario usuario, String slug, UUID itemId, Map<UUID, UUID> answers);
}
