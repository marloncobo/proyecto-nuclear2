package com.psicoapp.evaluation.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record DetalleEvaluacion(UUID idPregunta, UUID idOpcion, boolean contestada, boolean correcta,
        BigDecimal puntos, String retroalimentacion) { }
