package com.mundomanitas.api.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/** Formulario del cuestionario: diez preguntas, cuatro alternativas y una correcta. */
public class AdminCuestionarioFormulario {

    @Size(min = 10, max = 10, message = "El cuestionario debe tener diez preguntas.")
    private List<@Valid Pregunta> questions = new ArrayList<>();

    public List<Pregunta> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Pregunta> questions) {
        this.questions = questions;
    }

    /** Una pregunta de dos puntos con la alternativa correcta marcada por índice. */
    public static class Pregunta {

        @NotNull(message = "Ingresá la posición de la pregunta.")
        @Min(value = 1, message = "Las posiciones deben ir de 1 a 10 sin repetir.")
        @Max(value = 10, message = "Las posiciones deben ir de 1 a 10 sin repetir.")
        private Integer posicion;

        @NotBlank(message = "Ingresá el enunciado de la pregunta.")
        @Size(max = 500, message = "El enunciado no puede superar los 500 caracteres.")
        private String prompt;

        @NotNull(message = "Marcá una sola respuesta correcta por pregunta.")
        @Min(value = 1, message = "Marcá una sola respuesta correcta por pregunta.")
        @Max(value = 4, message = "Marcá una sola respuesta correcta por pregunta.")
        private Integer correctIndex;

        @Size(min = 4, max = 4, message = "Cada pregunta debe tener cuatro alternativas.")
        private List<@Valid Alternativa> alternatives = new ArrayList<>();

        public Integer getPosicion() {
            return posicion;
        }

        public void setPosicion(Integer posicion) {
            this.posicion = posicion;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public Integer getCorrectIndex() {
            return correctIndex;
        }

        public void setCorrectIndex(Integer correctIndex) {
            this.correctIndex = correctIndex;
        }

        public List<Alternativa> getAlternatives() {
            return alternatives;
        }

        public void setAlternatives(List<Alternativa> alternatives) {
            this.alternatives = alternatives;
        }
    }

    /** Texto de una alternativa; la corrección se elige en la pregunta. */
    public static class Alternativa {

        @NotBlank(message = "Ingresá el texto de la alternativa.")
        @Size(max = 300, message = "La alternativa no puede superar los 300 caracteres.")
        private String label;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
