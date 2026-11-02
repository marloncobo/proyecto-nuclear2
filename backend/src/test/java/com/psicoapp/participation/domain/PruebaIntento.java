package com.psicoapp.participation.domain;

import com.psicoapp.sessions.domain.ModoNavegacion;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PruebaIntento {
    private static final Instant INICIO = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID SESION = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID PREGUNTA_1 = UUID.randomUUID();
    private static final UUID PREGUNTA_2 = UUID.randomUUID();
    private static final UUID OPCION = UUID.randomUUID();

    @Test
    void iniciaConPreguntasOrdenadasYLimitePropio() {
        Intento intento = intento(ModoNavegacion.FLEXIBLE);

        assertEquals(EstadoIntento.EN_PROGRESO, intento.estado());
        assertEquals(INICIO.plusSeconds(1800), intento.limite());
        assertEquals(PREGUNTA_1, intento.preguntaActualId());
        assertTrue(intento.respuestas().isEmpty());
    }

    @Test
    void guardaLaUltimaRespuestaDeLaPreguntaActual() {
        Intento intento = intento(ModoNavegacion.FLEXIBLE)
                .responder(PREGUNTA_1, OPCION, INICIO.plusSeconds(1))
                .responder(PREGUNTA_1, OPCION, INICIO.plusSeconds(2));

        assertEquals(OPCION, intento.respuestas().get(PREGUNTA_1));
        assertEquals(0, intento.posicionActual());
    }

    @Test
    void modalidadRestringidaExigeRespuestaYNoPermiteRetroceso() {
        Intento intento = intento(ModoNavegacion.RESTRINGIDA);
        assertThrows(TransicionIntentoInvalida.class, () -> intento.avanzar(INICIO.plusSeconds(1)));

        Intento siguiente = intento.responder(PREGUNTA_1, OPCION, INICIO.plusSeconds(1))
                .avanzar(INICIO.plusSeconds(2));
        assertEquals(PREGUNTA_2, siguiente.preguntaActualId());
        assertThrows(TransicionIntentoInvalida.class, () -> siguiente.retroceder(INICIO.plusSeconds(3)));
    }

    @Test
    void modalidadFlexiblePermiteDejarPendienteYRetroceder() {
        Intento siguiente = intento(ModoNavegacion.FLEXIBLE).avanzar(INICIO.plusSeconds(1));
        assertEquals(PREGUNTA_2, siguiente.preguntaActualId());
        assertEquals(PREGUNTA_1, siguiente.retroceder(INICIO.plusSeconds(2)).preguntaActualId());
    }

    @Test
    void noPermiteResponderUnaPreguntaDistintaALaActual() {
        Intento intento = intento(ModoNavegacion.FLEXIBLE);

        assertThrows(TransicionIntentoInvalida.class,
                () -> intento.responder(PREGUNTA_2, OPCION, INICIO.plusSeconds(1)));
    }

    @Test
    void finalizadoNoPermiteCambiosNiReanudacion() {
        Intento finalizado = intento(ModoNavegacion.FLEXIBLE).finalizar(INICIO.plusSeconds(1));

        assertEquals(EstadoIntento.FINALIZADO, finalizado.estado());
        assertThrows(TransicionIntentoInvalida.class, () -> finalizado.reanudar(INICIO.plusSeconds(2)));
        assertThrows(TransicionIntentoInvalida.class,
                () -> finalizado.responder(PREGUNTA_1, OPCION, INICIO.plusSeconds(2)));
    }

    @Test
    void venceEnElInstanteLimiteYConservaRespuestas() {
        Intento intento = intento(ModoNavegacion.FLEXIBLE)
                .responder(PREGUNTA_1, OPCION, INICIO.plusSeconds(1));

        assertTrue(intento.estaVencido(intento.limite()));
        Intento vencido = intento.vencer(intento.limite());
        assertEquals(EstadoIntento.VENCIDO, vencido.estado());
        assertEquals(OPCION, vencido.respuestas().get(PREGUNTA_1));
    }

    @Test
    void rechazaOperacionesCuandoElTiempoSeAgota() {
        Intento intento = intento(ModoNavegacion.FLEXIBLE);

        assertThrows(TransicionIntentoInvalida.class,
                () -> intento.responder(PREGUNTA_1, OPCION, intento.limite()));
        assertThrows(TransicionIntentoInvalida.class, () -> intento.reanudar(intento.limite()));
    }

    private Intento intento(ModoNavegacion modo) {
        return Intento.iniciar(UUID.randomUUID(), SESION, ESTUDIANTE, INICIO, 30, modo,
                List.of(PREGUNTA_1, PREGUNTA_2));
    }
}
