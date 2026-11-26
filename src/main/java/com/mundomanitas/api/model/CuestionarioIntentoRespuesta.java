package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Respuesta elegida por pregunta dentro de un intento. */
@Entity
@Table(name = "quiz_attempt_answers")
public class CuestionarioIntentoRespuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "attempt_id", nullable = false)
    private UUID intentoId;

    @Column(name = "question_id", nullable = false)
    private UUID preguntaId;

    @Column(name = "alternative_id", nullable = false)
    private UUID alternativaId;

    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    protected CuestionarioIntentoRespuesta() {
    }

    public CuestionarioIntentoRespuesta(UUID intentoId, UUID preguntaId, UUID alternativaId,
                             int pointsEarned) {
        this.intentoId = intentoId;
        this.preguntaId = preguntaId;
        this.alternativaId = alternativaId;
        this.pointsEarned = pointsEarned;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIntentoId() {
        return intentoId;
    }

    public UUID getPreguntaId() {
        return preguntaId;
    }

    public UUID getAlternativaId() {
        return alternativaId;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }
}
