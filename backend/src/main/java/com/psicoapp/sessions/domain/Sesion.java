package com.psicoapp.sessions.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Contexto inmutable del patron State. No representa intentos individuales. */
public record Sesion(UUID id, UUID idCaso, UUID idEspacio, UUID idDocente,
        Instant inicioPrevisto, Instant finPrevisto, int duracionMaximaMin,
        ModoNavegacion modoNavegacion, String lugar, String mensaje, EstadoSesion estado) {
    public Sesion {
        if (id == null || idCaso == null || idEspacio == null || idDocente == null
                || inicioPrevisto == null || finPrevisto == null || modoNavegacion == null || estado == null) {
            throw new IllegalArgumentException("Los identificadores, fechas, modo y estado son obligatorios");
        }
        if (!inicioPrevisto.isBefore(finPrevisto)) {
            throw new IllegalArgumentException("El inicio debe ser anterior al fin previsto");
        }
        if (duracionMaximaMin <= 0) throw new IllegalArgumentException("La duracion debe ser positiva");
        // TIMESTAMP de MySQL conserva segundos; rechazar precision no representable.
        if (inicioPrevisto.getNano() != 0 || finPrevisto.getNano() != 0
                || inicioPrevisto.isBefore(Instant.parse("1970-01-01T00:00:01Z"))
                || finPrevisto.isAfter(Instant.parse("2038-01-19T03:14:07Z"))) {
            throw new IllegalArgumentException("Las fechas deben caber en TIMESTAMP MySQL, con precision de segundos");
        }
        lugar = textoOpcional(lugar, 160);
        mensaje = textoOpcional(mensaje, 16000);
    }

    public static Sesion programar(UUID id, UUID idCaso, UUID idEspacio, UUID idDocente,
            Instant inicio, Instant fin, int duracion, int tiempoEstimadoCaso,
            ModoNavegacion modo, String lugar, String mensaje) {
        if (duracion > tiempoEstimadoCaso) {
            throw new IllegalArgumentException("La duracion no puede superar el tiempo estimado del caso");
        }
        return new Sesion(id, idCaso, idEspacio, idDocente, inicio, fin, duracion, modo, lugar, mensaje, SinIniciar.INSTANCIA);
    }

    public Sesion iniciar(Instant ahora) {
        exigirInstante(ahora);
        return estado.iniciar(this, ahora);
    }

    public Sesion finalizar(MotivoCierre motivo, Instant ahora) {
        exigirInstante(ahora);
        if (motivo == null) throw new IllegalArgumentException("El motivo es obligatorio");
        return estado.finalizar(this, motivo, ahora);
    }

    public void validarParticipacion(Instant ahora) {
        exigirInstante(ahora);
        estado.validarParticipacion(this, ahora);
    }

    public boolean estaVencida(Instant ahora) {
        exigirInstante(ahora);
        return !ahora.isBefore(finPrevisto.plus(Duration.ofDays(1)));
    }

    Sesion conEstado(EstadoSesion nuevo) {
        return new Sesion(id, idCaso, idEspacio, idDocente, inicioPrevisto, finPrevisto,
                duracionMaximaMin, modoNavegacion, lugar, mensaje, nuevo);
    }

    private static void exigirInstante(Instant ahora) {
        if (ahora == null) throw new IllegalArgumentException("El instante es obligatorio");
    }

    private static String textoOpcional(String texto, int maximo) {
        if (texto == null) return null;
        String limpio = texto.strip();
        if (limpio.length() > maximo) throw new IllegalArgumentException("El texto supera la longitud permitida");
        return limpio.isEmpty() ? null : limpio;
    }
}
