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
    private final com.psicoapp.evaluation.application.GestionValoraciones valoraciones = mock(com.psicoapp.evaluation.application.GestionValoraciones.class);
    private final FachadaConsultaParticipaciones fachada = new FachadaConsultaParticipaciones(participaciones, resultados, valoraciones);
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
        verifyNoInteractions(resultados, valoraciones);
    }

    @Test
    void accesoDenegadoDetieneLaConsultaDeResultados() {
        when(participaciones.consultarPorEstudiante(docente, estudiante, caso))
                .thenThrow(new AccesoDenegado("Caso ajeno"));
        assertThrows(AccesoDenegado.class, () -> fachada.consultarPorEstudiante(docente, estudiante, caso));
        verifyNoInteractions(resultados, valoraciones);
    }

    @Test
    void incluyeValoracionesDeCadaIntentoSinPublicarAvisosDesdeLaLectura() {
        var intento = participacion("FINALIZADO", Map.of());
        var valoracion = new com.psicoapp.evaluation.application.GestionValoraciones.ValoracionPublicada(
                UUID.randomUUID(), intento.idIntento(), docente, "Valoracion formativa", Instant.EPOCH);
        when(participaciones.consultarPorEstudiante(docente, estudiante, caso)).thenReturn(List.of(intento));
        when(resultados.consultar(docente, intento.idIntento())).thenReturn(Optional.empty());
        when(valoraciones.consultar(docente, intento.idIntento())).thenReturn(List.of(valoracion));
        assertEquals(List.of(valoracion), fachada.consultarPorEstudiante(docente, estudiante, caso).getFirst().valoracionesGenerales());
        verify(valoraciones).consultar(docente, intento.idIntento());
        verifyNoMoreInteractions(valoraciones);
    }

    @Test
    void registrarDelegaEnElPropietarioSinDuplicarLaSolicitudDeAviso() {
        UUID intento = UUID.randomUUID();
        fachada.registrarValoracion(docente, intento, "Comentario");
        verify(valoraciones).registrar(docente, intento, "Comentario");
        verifyNoMoreInteractions(valoraciones);
        verifyNoInteractions(participaciones, resultados);
    }

    private ParticipacionFinalizada participacion(String estado, Map<UUID, UUID> respuestas) {
        return new ParticipacionFinalizada(UUID.randomUUID(), UUID.randomUUID(), estudiante,
                estado, Instant.EPOCH, Instant.EPOCH.plusSeconds(1800), List.copyOf(respuestas.keySet()), respuestas);
    }
}
