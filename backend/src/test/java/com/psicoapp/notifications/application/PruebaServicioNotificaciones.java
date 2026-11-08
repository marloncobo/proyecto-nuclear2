package com.psicoapp.notifications.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.notifications.domain.EstadoNotificacion;
import com.psicoapp.notifications.domain.Notificacion;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PruebaServicioNotificaciones {
    private static final Instant AHORA = Instant.parse("2026-01-01T10:00:00Z");
    private final UUID estudiante = UUID.randomUUID();
    private final List<Notificacion> almacen = new ArrayList<>();
    private ServicioNotificaciones servicio;

    @BeforeEach
    void preparar() {
        RepositorioNotificaciones repositorio = new RepositorioNotificaciones() {
            @Override public void guardar(Notificacion notificacion) { almacen.add(notificacion); }
            @Override public void actualizarEstado(UUID id, String estado) {
                for (int i = 0; i < almacen.size(); i++) {
                    if (almacen.get(i).id().equals(id)) {
                        almacen.set(i, almacen.get(i).marcar(EstadoNotificacion.valueOf(estado)));
                    }
                }
            }
            @Override public Optional<Notificacion> buscarPorId(UUID id) {
                return almacen.stream().filter(n -> n.id().equals(id)).findFirst();
            }
            @Override public List<Notificacion> listarPorDestinatario(UUID id) {
                return almacen.stream().filter(n -> n.idDestinatario().equals(id)).toList();
            }
        };
        DirectorioIdentidad identidades = new DirectorioIdentidad() {
            @Override public DatosUsuario exigirDocente(UUID id) { return null; }
            @Override public void exigirCreacionCasos(UUID id) { }
            @Override public void exigirEstudiante(UUID id) { }
            @Override public DatosUsuario registrarEstudiante(UUID id, String correo, String nombre) { return null; }
        };
        servicio = new ServicioNotificaciones(repositorio, n -> { }, identidades,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void creaYMarcaComoEnviadaUnaConvocatoria() {
        var resultado = servicio.crearConvocatoria(estudiante, "estudiante@universidad.edu.co",
                "Nueva convocatoria", "Tiene una actividad", "/sesiones/1", "abc123");

        assertEquals(EstadoNotificacion.ENVIADA.name(), resultado.estado());
        assertEquals(1, servicio.listar(estudiante, estudiante).size());
    }

    @Test
    void impideConsultarNotificacionesDeOtroEstudiante() {
        assertThrows(AccesoDenegado.class, () -> servicio.listar(UUID.randomUUID(), estudiante));
    }

    @Test
    void conservaElFalloDelCanalYPropagaLaExcepcion() {
        servicio = new ServicioNotificaciones(new RepositorioNotificaciones() {
            @Override public void guardar(Notificacion n) { almacen.add(n); }
            @Override public void actualizarEstado(UUID id, String estado) {
                almacen.set(0, almacen.get(0).marcar(EstadoNotificacion.valueOf(estado)));
            }
            @Override public Optional<Notificacion> buscarPorId(UUID id) { return Optional.empty(); }
            @Override public List<Notificacion> listarPorDestinatario(UUID id) { return List.of(); }
        }, n -> { throw new IllegalStateException("canal no disponible"); },
                new DirectorioIdentidad() {
                    @Override public DatosUsuario exigirDocente(UUID id) { return null; }
                    @Override public void exigirCreacionCasos(UUID id) { }
                    @Override public void exigirEstudiante(UUID id) { }
                    @Override public DatosUsuario registrarEstudiante(UUID id, String c, String n) { return null; }
                }, Clock.fixed(AHORA, ZoneOffset.UTC));

        assertThrows(IllegalStateException.class, () -> servicio.crearResultado(estudiante,
                "estudiante@universidad.edu.co", "Resultado", "Disponible", "/resultados/1"));
        assertEquals(EstadoNotificacion.FALLIDA, almacen.get(0).estado());
    }
}
