package com.psicoapp.evaluation.application;

import com.psicoapp.evaluation.domain.CalificacionPorPonderacion;
import com.psicoapp.evaluation.domain.PreguntaEvaluable;
import com.psicoapp.evaluation.domain.RubricaEvaluable;
import com.psicoapp.participation.application.RepositorioIntentos;
import com.psicoapp.participation.domain.EstadoIntento;
import com.psicoapp.participation.domain.Intento;
import com.psicoapp.sessions.domain.ModoNavegacion;
import com.psicoapp.shared.exceptions.Conflicto;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PruebaServicioEvaluacion {
    private static final Instant AHORA = Instant.parse("2026-01-01T10:00:00Z");
    private final UUID intentoId = UUID.randomUUID();
    private final UUID sesionId = UUID.randomUUID();
    private final UUID estudianteId = UUID.randomUUID();
    private final UUID preguntaId = UUID.randomUUID();
    private final UUID opcionId = UUID.randomUUID();
    private RepositorioIntentos intentos;
    private RepositorioEvaluaciones evaluaciones;
    private ServicioEvaluacion servicio;

    @BeforeEach
    void preparar() {
        Intento intento = Intento.iniciar(intentoId, sesionId, estudianteId, AHORA.minusSeconds(60), 30,
                ModoNavegacion.FLEXIBLE, List.of(preguntaId))
                .responder(preguntaId, opcionId, AHORA.minusSeconds(30))
                .finalizar(AHORA.minusSeconds(1));
        Map<UUID, Intento> almacen = new HashMap<>(Map.of(intentoId, intento));
        intentos = new RepositorioIntentos() {
            @Override public void guardar(Intento valor) { almacen.put(valor.id(), valor); }
            @Override public Optional<Intento> buscarPorId(UUID id) { return Optional.ofNullable(almacen.get(id)); }
            @Override public Optional<Intento> buscarActivo(UUID sesion, UUID estudiante) { return Optional.empty(); }
            @Override public void actualizar(Intento valor) { almacen.put(valor.id(), valor); }
            @Override public Optional<Intento> bloquearPorId(UUID id) { return buscarPorId(id); }
            @Override public List<Intento> buscarVencidos(Instant ahora) { return List.of(); }
        };
        ProveedorCasosEvaluables.CasoEvaluable caso = new ProveedorCasosEvaluables.CasoEvaluable(
                List.of(new PreguntaEvaluable(preguntaId,
                        List.of(new com.psicoapp.evaluation.domain.OpcionEvaluable(opcionId, true, "Correcta")))),
                new RubricaEvaluable(new BigDecimal("5.00"), List.of(
                        new com.psicoapp.evaluation.domain.CriterioEvaluable(BigDecimal.ONE, List.of(preguntaId)))));
        RepositorioEvaluaciones.EvaluacionPersistida[] guardada = new RepositorioEvaluaciones.EvaluacionPersistida[1];
        evaluaciones = new RepositorioEvaluaciones() {
            @Override public void guardar(EvaluacionPersistida evaluacion) { guardada[0] = evaluacion; }
            @Override public Optional<EvaluacionPersistida> buscarPorIntento(UUID id) {
                return Optional.ofNullable(guardada[0]);
            }
        };
        servicio = new ServicioEvaluacion(intentos, id -> caso, evaluaciones, new CalificacionPorPonderacion(),
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void evaluaUnaSolaVezYConservaElResultado() {
        ServicioEvaluacion.DatosEvaluacion resultado = servicio.evaluar(intentoId);

        assertEquals(new BigDecimal("5.00"), resultado.resultado().calificacion());
        assertEquals(EstadoIntento.FINALIZADO.name(), resultado.estadoIntento());
        assertSame(resultado.resultado(), servicio.evaluar(intentoId).resultado());
    }

    @Test
    void rechazaIntentoEnProgreso() {
        Intento enProgreso = Intento.iniciar(UUID.randomUUID(), sesionId, estudianteId, AHORA, 30,
                ModoNavegacion.FLEXIBLE, List.of(preguntaId));
        intentos.guardar(enProgreso);

        assertThrows(Conflicto.class, () -> servicio.evaluar(enProgreso.id()));
    }
}
