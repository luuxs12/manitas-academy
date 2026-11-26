package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Usuario;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cuestionario de un ejercicio: una sola entrega y resultado inmediato. */
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
                    IntentoVista intento, List<PreguntaVista> questions) {
    }

    /** Devuelve el cuestionario o el intento definitivo si ya fue enviado. */
    CuestionarioVista view(Usuario usuario, String slug, UUID itemId);

    /** Corrige y guarda el intento; el puntaje siempre lo calcula el servidor. */
    IntentoVista submit(Usuario usuario, String slug, UUID itemId, Map<UUID, UUID> answers);
}
