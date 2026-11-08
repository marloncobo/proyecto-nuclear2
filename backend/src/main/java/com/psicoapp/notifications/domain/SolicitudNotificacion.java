package com.psicoapp.notifications.domain;

import java.time.Instant;
import java.util.UUID;
import java.util.Objects;

/** Contenido de entrega; no registrar su representacion en logs. */
public record SolicitudNotificacion(UUID idEvento, UUID idUsuario, UUID idSesion, UUID idIntento,
        Canal canal, String tipo, String asunto, String mensaje, Instant fecha) {
    public enum Canal { CORREO, INTERNO }
    public SolicitudNotificacion {
        Objects.requireNonNull(idEvento); Objects.requireNonNull(idUsuario);
        Objects.requireNonNull(canal); Objects.requireNonNull(fecha);
        if (tipo == null || tipo.isBlank() || asunto == null || asunto.isBlank()
                || mensaje == null || mensaje.isBlank()) throw new IllegalArgumentException("Contenido obligatorio");
        if (asunto.contains("\r") || asunto.contains("\n")) throw new IllegalArgumentException("Asunto invalido");
    }
    @Override public String toString() {
        return "SolicitudNotificacion[idEvento=" + idEvento + ", canal=" + canal + "]";
    }
}
