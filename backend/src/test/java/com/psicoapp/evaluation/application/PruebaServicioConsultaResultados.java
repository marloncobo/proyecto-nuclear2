package com.psicoapp.evaluation.application;

import com.psicoapp.evaluation.domain.DetalleEvaluacion;
import com.psicoapp.evaluation.domain.ResultadoEvaluacion;
import com.psicoapp.participation.application.ConsultaParticipaciones;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.Conflicto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RF-15 / RN-08/09/16: resultados almacenados para el docente responsable")
class PruebaServicioConsultaResultados {
    private final ConsultaParticipaciones participaciones = mock(ConsultaParticipaciones.class);
    private final RepositorioEvaluaciones evaluaciones = mock(RepositorioEvaluaciones.class);
    private final ServicioConsultaResultados servicio = new ServicioConsultaResultados(participaciones, evaluaciones);
    private final UUID docente = UUID.randomUUID();
    private final UUID intento = UUID.randomUUID();

    @Test
    void exponeNotaYRetroalimentacionIncluyendoPreguntasSinRespuesta() {
        UUID pregunta = UUID.randomUUID();
        var resultado = new ResultadoEvaluacion(BigDecimal.ZERO,
                List.of(new DetalleEvaluacion(pregunta, null, false, false, BigDecimal.ZERO, "Sin respuesta")));
        when(evaluaciones.buscarPorIntento(intento)).thenReturn(Optional.of(
                new RepositorioEvaluaciones.EvaluacionPersistida(intento, resultado, Instant.EPOCH)));
        var publicado = servicio.consultar(docente, intento).orElseThrow();
        assertEquals(BigDecimal.ZERO, publicado.calificacion());
        assertEquals(Instant.EPOCH, publicado.fecha());
        assertNull(publicado.detalles().getFirst().idOpcion());
        assertFalse(publicado.detalles().getFirst().contestada());
        assertEquals("Sin respuesta", publicado.detalles().getFirst().retroalimentacion());
        var orden = inOrder(participaciones, evaluaciones);
        orden.verify(participaciones).exigirFinalizada(docente, intento);
        orden.verify(evaluaciones).buscarPorIntento(intento);
        verifyNoMoreInteractions(evaluaciones);
    }

    @Test
    void noCalculaEvaluacionesPendientes() {
        when(evaluaciones.buscarPorIntento(intento)).thenReturn(Optional.empty());
        assertTrue(servicio.consultar(docente, intento).isEmpty());
        verify(evaluaciones).buscarPorIntento(intento);
        verifyNoMoreInteractions(evaluaciones);
    }

    @Test
    void noLeeResultadosDeIntentosAjenosOEnProgreso() {
        for (RuntimeException error : List.of(new AccesoDenegado("Ajeno"), new Conflicto("En progreso"))) {
            doThrow(error).when(participaciones).exigirFinalizada(docente, intento);
            assertThrows(error.getClass(), () -> servicio.consultar(docente, intento));
        }
        verifyNoInteractions(evaluaciones);
    }
}
