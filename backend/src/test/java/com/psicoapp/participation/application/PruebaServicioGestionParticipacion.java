package com.psicoapp.participation.application;

import com.psicoapp.participation.domain.EstadoIntento;
import com.psicoapp.participation.domain.Intento;
import com.psicoapp.sessions.application.AutorizacionConvocatorias;
import com.psicoapp.sessions.application.ConsultaSesiones;
import com.psicoapp.sessions.domain.ModoNavegacion;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.Conflicto;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PruebaServicioGestionParticipacion {
    private static final Instant AHORA = Instant.parse("2026-01-01T10:00:00Z");
    private final UUID estudiante = UUID.randomUUID();
    private final UUID otroEstudiante = UUID.randomUUID();
    private final UUID sesion = UUID.randomUUID();
    private final UUID pregunta = UUID.randomUUID();
    private final UUID opcion = UUID.randomUUID();
    private RepositorioEnMemoria repositorio;
    private ServicioGestionParticipacion servicio;
    private int accesosVerificados;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEnMemoria();
        AutorizacionConvocatorias autorizacion = (idEstudiante, idSesion, codigo) -> accesosVerificados++;
        ConsultaSesiones sesiones = idSesion -> new ConsultaSesiones.SesionEnCurso(sesion, UUID.randomUUID(),
                AHORA.minusSeconds(60), AHORA.plusSeconds(3600), 30, ModoNavegacion.FLEXIBLE.name());
        servicio = new ServicioGestionParticipacion(repositorio, autorizacion, sesiones,
                Clock.fixed(AHORA, ZoneOffset.UTC), org.mockito.Mockito.mock(com.psicoapp.shared.events.NotificacionSujeto.class));
    }

    @Test
    void iniciaUnIntentoDespuesDeVerificarAcceso() {
        ServicioGestionParticipacion.DatosIntento datos = servicio.iniciar(estudiante, sesion, "codigo",
                List.of(pregunta));

        assertEquals(1, accesosVerificados);
        assertEquals(EstadoIntento.EN_PROGRESO.name(), datos.estado());
        assertEquals(estudiante, datos.idEstudiante());
        assertEquals(1, repositorio.intentos.size());
    }

    @Test
    void impideDosIntentosActivosParaLaMismaSesionYEstudiante() {
        servicio.iniciar(estudiante, sesion, "codigo", List.of(pregunta));

        assertThrows(Conflicto.class,
                () -> servicio.iniciar(estudiante, sesion, "codigo", List.of(pregunta)));
    }

    @Test
    void soloElPropietarioPuedeResponder() {
        UUID idIntento = servicio.iniciar(estudiante, sesion, "codigo", List.of(pregunta)).id();

        assertThrows(AccesoDenegado.class,
                () -> servicio.responder(otroEstudiante, idIntento, pregunta, opcion));
    }

    @Test
    void respondeYFinalizaElIntento() {
        UUID idIntento = servicio.iniciar(estudiante, sesion, "codigo", List.of(pregunta)).id();

        ServicioGestionParticipacion.DatosIntento respondido =
                servicio.responder(estudiante, idIntento, pregunta, opcion);
        ServicioGestionParticipacion.DatosIntento finalizado = servicio.finalizar(estudiante, idIntento);

        assertEquals(opcion, respondido.respuestas().get(pregunta));
        assertEquals(EstadoIntento.FINALIZADO.name(), finalizado.estado());
    }

    @Test
    void finalizaIntentosVencidosConservandoLaRespuesta() {
        UUID idIntento = servicio.iniciar(estudiante, sesion, "codigo", List.of(pregunta)).id();
        servicio.responder(estudiante, idIntento, pregunta, opcion);
        Intento almacenado = repositorio.intentos.get(idIntento);
        repositorio.intentos.put(idIntento, Intento.iniciar(almacenado.id(), almacenado.idSesion(),
                almacenado.idEstudiante(), AHORA.minusSeconds(1801), 30, almacenado.modoNavegacion(),
                almacenado.preguntas()).responder(pregunta, opcion, AHORA.minusSeconds(2)));

        assertEquals(1, servicio.finalizarVencidos(AHORA));
        assertEquals(EstadoIntento.VENCIDO, repositorio.intentos.get(idIntento).estado());
        assertEquals(opcion, repositorio.intentos.get(idIntento).respuestas().get(pregunta));
    }

    private static final class RepositorioEnMemoria implements RepositorioIntentos {
        private final Map<UUID, Intento> intentos = new HashMap<>();

        @Override public void guardar(Intento intento) { intentos.put(intento.id(), intento); }
        @Override public Optional<Intento> buscarPorId(UUID id) { return Optional.ofNullable(intentos.get(id)); }
        @Override public Optional<Intento> buscarActivo(UUID idSesion, UUID idEstudiante) {
            return intentos.values().stream().filter(i -> i.idSesion().equals(idSesion)
                    && i.idEstudiante().equals(idEstudiante)
                    && i.estado() == EstadoIntento.EN_PROGRESO).findFirst();
        }
        @Override public void actualizar(Intento intento) { intentos.put(intento.id(), intento); }
        @Override public Optional<Intento> bloquearPorId(UUID id) { return buscarPorId(id); }
        @Override public List<Intento> buscarVencidos(Instant ahora) {
            return new ArrayList<>(intentos.values()).stream().filter(i -> i.estaVencido(ahora)
                    && i.estado() == EstadoIntento.EN_PROGRESO).toList();
        }
    }
}
