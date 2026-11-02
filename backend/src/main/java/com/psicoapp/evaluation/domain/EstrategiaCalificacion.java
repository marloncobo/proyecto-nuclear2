package com.psicoapp.evaluation.domain;

import java.util.Map;
import java.util.UUID;

public interface EstrategiaCalificacion {
    ResultadoEvaluacion calificar(Iterable<PreguntaEvaluable> preguntas,
            Map<UUID, UUID> respuestas, RubricaEvaluable rubrica);
}
