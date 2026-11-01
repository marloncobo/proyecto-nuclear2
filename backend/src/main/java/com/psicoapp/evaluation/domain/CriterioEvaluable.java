package com.psicoapp.evaluation.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CriterioEvaluable(BigDecimal ponderacion, List<UUID> preguntas) {
    public CriterioEvaluable {
        Objects.requireNonNull(ponderacion, "La ponderacion es obligatoria");
        if (ponderacion.signum() < 0) throw new IllegalArgumentException("La ponderacion no puede ser negativa");
        if (preguntas == null || preguntas.isEmpty()) throw new IllegalArgumentException("El criterio requiere preguntas");
        if (preguntas.stream().anyMatch(Objects::isNull)
                || preguntas.stream().distinct().count() != preguntas.size()) {
            throw new IllegalArgumentException("Las preguntas del criterio son invalidas");
        }
        preguntas = List.copyOf(preguntas);
    }
}
