package com.psicoapp.evaluation.domain;

import java.util.Objects;
import java.util.UUID;

public record OpcionEvaluable(UUID id, boolean correcta, String justificacion) {
    public OpcionEvaluable {
        Objects.requireNonNull(id, "La opcion es obligatoria");
        if (justificacion == null || justificacion.isBlank()) {
            throw new IllegalArgumentException("La justificacion es obligatoria");
        }
    }
}
