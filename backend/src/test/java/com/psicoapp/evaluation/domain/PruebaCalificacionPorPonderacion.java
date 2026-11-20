package com.psicoapp.evaluation.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PruebaCalificacionPorPonderacion {
    private final UUID pregunta1 = UUID.randomUUID();
    private final UUID pregunta2 = UUID.randomUUID();
    private final UUID correcta = UUID.randomUUID();
    private final UUID incorrecta = UUID.randomUUID();

    @Test
    void calculaLaEscalaConPonderacionesYRetroalimentacion() {
        ResultadoEvaluacion resultado = evaluar(Map.of(pregunta1, correcta, pregunta2, incorrecta));

        assertEquals(new BigDecimal("3.00"), resultado.calificacion());
        assertTrue(resultado.detalles().get(0).correcta());
        assertFalse(resultado.detalles().get(1).correcta());
        assertEquals("Bien", resultado.detalles().get(0).retroalimentacion());
    }

    @Test
    void asignaCeroAPreguntaSinResponder() {
        ResultadoEvaluacion resultado = evaluar(Map.of(pregunta1, correcta));

        assertEquals(new BigDecimal("3.00"), resultado.calificacion());
        DetalleEvaluacion detalle = resultado.detalles().get(1);
        assertFalse(detalle.contestada());
        assertEquals(BigDecimal.ZERO.setScale(2), detalle.puntos());
        assertEquals("Pregunta sin responder", detalle.retroalimentacion());
    }

    @Test
    void rechazaUnaOpcionQueNoPerteneceALaPregunta() {
        assertThrows(IllegalArgumentException.class,
                () -> evaluar(Map.of(pregunta1, UUID.randomUUID())));
    }

    @Test
    void reparteElPesoDeUnCriterioEntreSusPreguntas() {
        var opcion = new OpcionEvaluable(correcta, true, "Bien");
        var preguntas = List.of(new PreguntaEvaluable(pregunta1, List.of(opcion)),
                new PreguntaEvaluable(pregunta2, List.of(opcion)));
        var rubrica = new RubricaEvaluable(new BigDecimal("5.00"), List.of(
                new CriterioEvaluable(BigDecimal.ONE, List.of(pregunta1, pregunta2))));
        var resultado = new CalificacionPorPonderacion().calificar(
                preguntas, Map.of(pregunta1, correcta, pregunta2, correcta), rubrica);

        assertEquals(new BigDecimal("5.00"), resultado.calificacion());
        assertEquals(new BigDecimal("2.50"), resultado.detalles().getFirst().puntos());
    }

    private ResultadoEvaluacion evaluar(Map<UUID, UUID> respuestas) {
        PreguntaEvaluable primera = new PreguntaEvaluable(pregunta1,
                List.of(new OpcionEvaluable(correcta, true, "Bien"),
                        new OpcionEvaluable(incorrecta, false, "Revisa")));
        PreguntaEvaluable segunda = new PreguntaEvaluable(pregunta2,
                List.of(new OpcionEvaluable(correcta, true, "Bien"),
                        new OpcionEvaluable(incorrecta, false, "Revisa")));
        RubricaEvaluable rubrica = new RubricaEvaluable(new BigDecimal("6.00"), List.of(
                new CriterioEvaluable(new BigDecimal("0.50"), List.of(pregunta1)),
                new CriterioEvaluable(new BigDecimal("0.50"), List.of(pregunta2))));
        return new CalificacionPorPonderacion().calificar(List.of(primera, segunda), respuestas, rubrica);
    }
}
