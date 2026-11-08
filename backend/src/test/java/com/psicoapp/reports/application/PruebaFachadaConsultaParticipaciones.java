package com.psicoapp.reports.application;

import com.psicoapp.evaluation.application.ConsultaResultados;
import com.psicoapp.participation.application.ConsultaParticipaciones;
import com.psicoapp.participation.application.ConsultaParticipaciones.ParticipacionFinalizada;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RF-15 / CU-30 / RN-06: fachada de seguimiento docente")
class PruebaFachadaConsultaParticipaciones {
    private final ConsultaParticipaciones participaciones = mock(ConsultaParticipaciones.class);
    private final ConsultaResultados resultados = mock(ConsultaResultados.class);
    private final FachadaConsultaParticipaciones fachada = new FachadaConsultaParticipaciones(participaciones, resultados);
    private final UUID docente = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();
    private final UUID caso = UUID.randomUUID();

    @Test
    void conservaIntentosIndependientesYEvaluacionPendiente() {
        UUID pregunta = UUID.randomUUID();
        UUID opcion = UUID.randomUUID();
        var primero = participacion("FINALIZADO", Map.of(pregunta, opcion));
        var segundo = participacion("VENCIDO", Map.of());
        var resultado = new ConsultaResultados.ResultadoPublicado(new BigDecimal("4.00"), Instant.EPOCH,
                List.of(new ConsultaResultados.RespuestaEvaluada(pregunta, opcion, true, true,
                        BigDecimal.ONE, "Explicacion")));
        when(participaciones.consultarPorEstudiante(docente, estudiante, caso)).thenReturn(List.of(primero, segundo));
        when(resultados.consultar(docente, primero.idIntento())).thenReturn(Optional.of(resultado));
        when(resultados.consultar(docente, segundo.idIntento())).thenReturn(Optional.empty());

        var resumen = fachada.consultarPorEstudiante(docente, estudiante, caso);

        assertEquals(2, resumen.size());
        assertEquals(opcion, resumen.get(0).participacion().respuestas().get(pregunta));
        assertEquals(resultado, resumen.get(0).resultado());
        assertEquals(segundo.idIntento(), resumen.get(1).participacion().idIntento());
        assertNull(resumen.get(1).resultado());
    }

    @Test
    void sinIntentosDevuelveListaVaciaSinInventarAusencias() {
        when(participaciones.consultarPorEstudiante(docente, estudiante, caso)).thenReturn(List.of());
        assertTrue(fachada.consultarPorEstudiante(docente, estudiante, caso).isEmpty());
        verifyNoInteractions(resultados);
    }

    @Test
    void accesoDenegadoDetieneLaConsultaDeResultados() {
        when(participaciones.consultarPorEstudiante(docente, estudiante, caso))
                .thenThrow(new AccesoDenegado("Caso ajeno"));
        assertThrows(AccesoDenegado.class, () -> fachada.consultarPorEstudiante(docente, estudiante, caso));
        verifyNoInteractions(resultados);
    }

    private ParticipacionFinalizada participacion(String estado, Map<UUID, UUID> respuestas) {
        return new ParticipacionFinalizada(UUID.randomUUID(), UUID.randomUUID(), estudiante,
                estado, Instant.EPOCH, Instant.EPOCH.plusSeconds(1800), List.copyOf(respuestas.keySet()), respuestas);
    }
}
