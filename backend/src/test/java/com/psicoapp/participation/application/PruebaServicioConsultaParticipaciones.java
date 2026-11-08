package com.psicoapp.participation.application;

import com.psicoapp.cases.application.ConsultaCasosDocente;
import com.psicoapp.participation.domain.Intento;
import com.psicoapp.sessions.application.ConsultaSesionesDocente;
import com.psicoapp.sessions.domain.ModoNavegacion;
import com.psicoapp.shared.exceptions.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RF-15 / RN-06/08: consulta autorizada de participaciones terminadas")
class PruebaServicioConsultaParticipaciones {
    private final RepositorioConsultaIntentos consultas = mock(RepositorioConsultaIntentos.class);
    private final RepositorioIntentos intentos = mock(RepositorioIntentos.class);
    private final ConsultaCasosDocente casos = mock(ConsultaCasosDocente.class);
    private final ConsultaSesionesDocente sesiones = mock(ConsultaSesionesDocente.class);
    private final ServicioConsultaParticipaciones servicio =
            new ServicioConsultaParticipaciones(consultas, intentos, casos, sesiones);
    private final UUID docente = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();
    private final UUID caso = UUID.randomUUID();
    private final Intento intento = Intento.iniciar(UUID.randomUUID(), UUID.randomUUID(), estudiante,
            Instant.EPOCH, 30, ModoNavegacion.FLEXIBLE, List.of(UUID.randomUUID()));

    @Test
    void autorizaAntesDeLeerYConservaPreguntasNoRespondidas() {
        when(consultas.buscarTerminadosPorEstudianteYCaso(estudiante, caso))
                .thenReturn(List.of(intento.finalizar(Instant.EPOCH.plusSeconds(1))));
        var resultado = servicio.consultarPorEstudiante(docente, estudiante, caso);
        var orden = inOrder(casos, consultas);
        orden.verify(casos).exigirResponsable(docente, caso);
        orden.verify(consultas).buscarTerminadosPorEstudianteYCaso(estudiante, caso);
        assertEquals(intento.preguntas(), resultado.getFirst().preguntas());
        assertTrue(resultado.getFirst().respuestas().isEmpty());
    }

    @Test
    void rechazaCasoAjenoAntesDeLeerIntentos() {
        doThrow(new AccesoDenegado("Caso ajeno")).when(casos).exigirResponsable(docente, caso);
        assertThrows(AccesoDenegado.class, () -> servicio.consultarPorEstudiante(docente, estudiante, caso));
        verifyNoInteractions(consultas, intentos, sesiones);
    }

    @Test
    void exigeSesionPropiaYBloqueaIntentosEnProgreso() {
        when(intentos.buscarPorId(intento.id())).thenReturn(Optional.of(intento));
        assertThrows(Conflicto.class, () -> servicio.exigirFinalizada(docente, intento.id()));
        verify(sesiones).exigirResponsable(docente, intento.idSesion());
        doThrow(new AccesoDenegado("Sesion ajena")).when(sesiones).exigirResponsable(docente, intento.idSesion());
        assertThrows(AccesoDenegado.class, () -> servicio.exigirFinalizada(docente, intento.id()));
    }

    @Test
    void permiteVencidosYRechazaIntentosInexistentes() {
        when(intentos.buscarPorId(intento.id())).thenReturn(Optional.of(intento.vencer(intento.limite())));
        assertEquals("VENCIDO", servicio.exigirFinalizada(docente, intento.id()).estado());
        assertThrows(NoEncontrado.class, () -> servicio.exigirFinalizada(docente, UUID.randomUUID()));
    }
}
