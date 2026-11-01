package com.psicoapp.sessions.domain;

import com.psicoapp.sessions.DatosPruebaSesiones;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static com.psicoapp.sessions.DatosPruebaSesiones.*;
import static org.junit.jupiter.api.Assertions.*;

class PruebaSesion {
    private final Sesion sesion = DatosPruebaSesiones.programada(UUID.randomUUID());

    @Test
    @DisplayName("RF-19 / RN-12: SIN_INICIAR -> EN_CURSO -> FINALIZADO con cierre idempotente")
    void transitaPorEstadosSinModificarLaSesionAnterior() {
        assertSame(SinIniciar.INSTANCIA, sesion.estado());
        Sesion iniciada = sesion.iniciar(INICIO);
        assertSame(EnCurso.INSTANCIA, iniciada.estado());
        assertSame(SinIniciar.INSTANCIA, sesion.estado());
        assertDoesNotThrow(() -> iniciada.validarParticipacion(INICIO));
        Sesion cerrada = iniciada.finalizar(MotivoCierre.DOCENTE, INICIO);
        assertSame(Finalizado.INSTANCIA, cerrada.estado());
        assertSame(cerrada, cerrada.finalizar(MotivoCierre.DOCENTE, FIN));
        assertSame(cerrada, cerrada.finalizar(MotivoCierre.AUTOMATICO, FIN));
    }

    @Test
    @DisplayName("RN-12 / RF-25: no participa sin iniciar ni finalizada; no reinicia")
    void rechazaOperacionesProhibidas() {
        assertThrows(TransicionSesionInvalida.class, () -> sesion.validarParticipacion(INICIO));
        assertThrows(TransicionSesionInvalida.class, () -> sesion.finalizar(MotivoCierre.DOCENTE, INICIO));
        Sesion iniciada = sesion.iniciar(INICIO);
        assertThrows(TransicionSesionInvalida.class, () -> iniciada.iniciar(INICIO));
        Sesion cerrada = iniciada.finalizar(MotivoCierre.DOCENTE, FIN);
        assertThrows(TransicionSesionInvalida.class, () -> cerrada.iniciar(FIN));
        assertThrows(TransicionSesionInvalida.class, () -> cerrada.validarParticipacion(FIN));
    }

    @Test
    @DisplayName("RN-12: cierre automatico exacto a fin previsto + 24 horas en ambos estados abiertos")
    void verificaFronteraDeCierreAutomatico() {
        var limite = FIN.plus(Duration.ofDays(1));
        for (Sesion abierta : new Sesion[]{sesion, sesion.iniciar(INICIO)}) {
            assertFalse(abierta.estaVencida(limite.minusNanos(1)));
            assertThrows(TransicionSesionInvalida.class,
                    () -> abierta.finalizar(MotivoCierre.AUTOMATICO, limite.minusNanos(1)));
            assertTrue(abierta.estaVencida(limite));
            assertEquals(TipoEstadoSesion.FINALIZADO, abierta.finalizar(MotivoCierre.AUTOMATICO, limite).estado().codigo());
        }
    }

    @Test
    @DisplayName("RN-12: no permite ingreso ni inicio vencido aunque el planificador se retrase")
    void impideOperacionesVencidasAntesDelBarrido() {
        var limite = FIN.plus(Duration.ofDays(1));
        assertThrows(TransicionSesionInvalida.class, () -> sesion.iniciar(limite));
        Sesion iniciada = sesion.iniciar(INICIO);
        assertDoesNotThrow(() -> iniciada.validarParticipacion(limite.minusNanos(1)));
        assertThrows(TransicionSesionInvalida.class, () -> iniciada.validarParticipacion(limite));
    }

    @Test
    @DisplayName("RF-18 / RN-13: ventana coherente y duracion positiva no mayor a estimada")
    void validaProgramacion() {
        assertThrows(IllegalArgumentException.class, () -> Sesion.programar(UUID.randomUUID(), sesion.idCaso(),
                sesion.idEspacio(), sesion.idDocente(), FIN, INICIO, 30, 40, ModoNavegacion.FLEXIBLE, null, null));
        for (int duracion : new int[]{0, -1, 41}) {
            assertThrows(IllegalArgumentException.class, () -> Sesion.programar(UUID.randomUUID(), sesion.idCaso(),
                    sesion.idEspacio(), sesion.idDocente(), INICIO, FIN, duracion, 40, ModoNavegacion.FLEXIBLE, null, null));
        }
        assertDoesNotThrow(() -> Sesion.programar(UUID.randomUUID(), sesion.idCaso(), sesion.idEspacio(),
                sesion.idDocente(), INICIO, FIN, 40, 40, ModoNavegacion.RESTRINGIDA, " Aula ", " Mensaje "));
    }

    @Test
    @DisplayName("RF-18: rechaza datos nulos, textos fuera de limites y precision temporal no persistible")
    void validaDatosYReconstruccion() {
        assertThrows(IllegalArgumentException.class, () -> Sesion.programar(null, sesion.idCaso(), sesion.idEspacio(),
                sesion.idDocente(), INICIO, FIN, 30, 40, ModoNavegacion.FLEXIBLE, null, null));
        assertThrows(IllegalArgumentException.class, () -> Sesion.programar(UUID.randomUUID(), sesion.idCaso(), sesion.idEspacio(),
                sesion.idDocente(), INICIO.plusNanos(1), FIN, 30, 40, ModoNavegacion.FLEXIBLE, null, null));
        assertThrows(IllegalArgumentException.class, () -> Sesion.programar(UUID.randomUUID(), sesion.idCaso(), sesion.idEspacio(),
                sesion.idDocente(), INICIO, FIN, 30, 40, ModoNavegacion.FLEXIBLE, "x".repeat(161), null));
        for (TipoEstadoSesion codigo : TipoEstadoSesion.values()) assertEquals(codigo, EstadoSesion.desde(codigo).codigo());
        assertThrows(IllegalArgumentException.class, () -> EstadoSesion.desde(null));
    }
}
