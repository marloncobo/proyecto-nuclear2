package com.psicoapp.student.application;

import com.psicoapp.cases.application.RepositorioCasos;
import com.psicoapp.cases.domain.Caso;
import com.psicoapp.cases.domain.EstadoCaso;
import com.psicoapp.evaluation.application.RepositorioEvaluaciones;
import com.psicoapp.evaluation.application.RepositorioValoraciones;
import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.participation.application.RepositorioIntentos;
import com.psicoapp.participation.application.ServicioGestionParticipacion;
import com.psicoapp.sessions.application.RepositorioConvocatorias;
import com.psicoapp.sessions.application.RepositorioSesiones;
import com.psicoapp.sessions.domain.Convocatoria;
import com.psicoapp.sessions.domain.ModoNavegacion;
import com.psicoapp.sessions.domain.Sesion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PruebaServicioEspacioEstudiante {
    @Mock DirectorioIdentidad identidades;
    @Mock RepositorioConvocatorias convocatorias;
    @Mock RepositorioSesiones sesiones;
    @Mock RepositorioCasos casos;
    @Mock RepositorioIntentos intentos;
    @Mock RepositorioEvaluaciones evaluaciones;
    @Mock RepositorioValoraciones valoraciones;
    @Mock ServicioGestionParticipacion participaciones;
    UUID estudiante;
    Sesion sesion;
    Caso caso;
    ServicioEspacioEstudiante servicio;

    @BeforeEach
    void preparar() {
        estudiante = UUID.randomUUID();
        UUID idCaso = UUID.randomUUID();
        sesion = Sesion.programar(UUID.randomUUID(), idCaso, UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-11-24T18:00:00Z"), Instant.parse("2026-11-24T20:00:00Z"),
                30, 45, ModoNavegacion.FLEXIBLE, "Aula", "Mensaje")
                .iniciar(Instant.parse("2026-11-24T18:00:00Z"));
        caso = new Caso(idCaso, sesion.idDocente(), "Caso", "Desarrollo", "Contexto", 45,
                List.of(), null, EstadoCaso.PUBLICADO);
        servicio = new ServicioEspacioEstudiante(identidades, convocatorias, sesiones, casos, intentos,
                evaluaciones, valoraciones, participaciones,
                Clock.fixed(Instant.parse("2026-11-24T18:10:00Z"), ZoneOffset.UTC));
        when(convocatorias.buscarAutorizada(estudiante, sesion.id())).thenReturn(Optional.of(
                new Convocatoria(UUID.randomUUID(), sesion.id(), estudiante, "a".repeat(64), true)));
        when(sesiones.buscarPorId(sesion.id())).thenReturn(Optional.of(sesion));
        when(casos.buscarPorId(caso.id())).thenReturn(Optional.of(caso));
        when(intentos.buscarActivo(sesion.id(), estudiante)).thenReturn(Optional.empty());
        when(intentos.buscarUltimo(sesion.id(), estudiante)).thenReturn(Optional.empty());
    }

    @Test
    void consultarActividadNoCreaIntentoYOfreceInicioExplicito() {
        var datos = servicio.consultarActividad(estudiante, sesion.id());
        assertNull(datos.intento());
        assertEquals("INICIAR", datos.actividad().accion());
        verify(participaciones).consultarParaSesion(estudiante, sesion.id());
        verify(participaciones, never()).iniciar(any(), any());
    }

    @Test
    void iniciarDelegaSoloTrasLaAccionExplicita() {
        servicio.iniciar(estudiante, sesion.id());
        verify(participaciones).iniciar(estudiante, sesion.id());
    }
}
