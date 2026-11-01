package com.psicoapp.evaluation.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record RubricaEvaluable(BigDecimal escalaMaxima, List<CriterioEvaluable> criterios) {
    public RubricaEvaluable {
        Objects.requireNonNull(escalaMaxima, "La escala maxima es obligatoria");
        if (escalaMaxima.signum() <= 0) throw new IllegalArgumentException("La escala maxima debe ser positiva");
        if (criterios == null || criterios.isEmpty()) throw new IllegalArgumentException("La rubrica requiere criterios");
        criterios = List.copyOf(criterios);
    }
}
