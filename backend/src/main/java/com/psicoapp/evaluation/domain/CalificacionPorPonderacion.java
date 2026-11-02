package com.psicoapp.evaluation.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CalificacionPorPonderacion implements EstrategiaCalificacion {
    private static final int ESCALA_DECIMALES = 2;

    @Override
    public ResultadoEvaluacion calificar(Iterable<PreguntaEvaluable> preguntas,
            Map<UUID, UUID> respuestas, RubricaEvaluable rubrica) {
        if (preguntas == null || respuestas == null || rubrica == null) {
            throw new IllegalArgumentException("Preguntas, respuestas y rubrica son obligatorias");
        }
        Map<UUID, BigDecimal> ponderaciones = ponderacionesPorPregunta(rubrica);
        List<DetalleEvaluacion> detalles = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (PreguntaEvaluable pregunta : preguntas) {
            UUID idOpcion = respuestas.get(pregunta.id());
            OpcionEvaluable opcion = idOpcion == null ? null : pregunta.opcion(idOpcion);
            if (idOpcion != null && opcion == null) {
                throw new IllegalArgumentException("La respuesta no pertenece a la pregunta");
            }
            boolean contestada = opcion != null;
            boolean correcta = contestada && opcion.correcta();
            BigDecimal puntos = correcta
                    ? rubrica.escalaMaxima().multiply(ponderaciones.getOrDefault(pregunta.id(), BigDecimal.ZERO))
                    : BigDecimal.ZERO;
            if (correcta) total = total.add(puntos);
            String retroalimentacion = contestada ? opcion.justificacion() : "Pregunta sin responder";
            detalles.add(new DetalleEvaluacion(pregunta.id(), idOpcion, contestada, correcta,
                    puntos.setScale(ESCALA_DECIMALES, RoundingMode.HALF_UP), retroalimentacion));
        }
        return new ResultadoEvaluacion(total.setScale(ESCALA_DECIMALES, RoundingMode.HALF_UP), detalles);
    }

    private Map<UUID, BigDecimal> ponderacionesPorPregunta(RubricaEvaluable rubrica) {
        Map<UUID, BigDecimal> ponderaciones = new HashMap<>();
        for (CriterioEvaluable criterio : rubrica.criterios()) {
            for (UUID idPregunta : criterio.preguntas()) {
                ponderaciones.merge(idPregunta, criterio.ponderacion(), BigDecimal::add);
            }
        }
        return ponderaciones;
    }
}
