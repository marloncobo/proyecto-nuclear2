package com.psicoapp.evaluation.domain;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoEvaluacion(BigDecimal calificacion, List<DetalleEvaluacion> detalles) {
    public ResultadoEvaluacion {
        if (calificacion == null || calificacion.signum() < 0) {
            throw new IllegalArgumentException("La calificacion no puede ser negativa");
        }
        detalles = List.copyOf(detalles);
    }
}
