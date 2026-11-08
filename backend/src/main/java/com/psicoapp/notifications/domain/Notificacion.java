package com.psicoapp.notifications.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Notificacion(UUID id, UUID idDestinatario, String correoDestinatario,
        TipoNotificacion tipo, String asunto, String mensaje, String url, String codigoAcceso,
        Instant fechaCreacion, EstadoNotificacion estado) {
    public Notificacion {
        Objects.requireNonNull(id, "La notificacion es obligatoria");
        Objects.requireNonNull(idDestinatario, "El destinatario es obligatorio");
        if (correoDestinatario == null || correoDestinatario.isBlank()) {
            throw new IllegalArgumentException("El correo del destinatario es obligatorio");
        }
        Objects.requireNonNull(tipo, "El tipo es obligatorio");
        if (asunto == null || asunto.isBlank()) throw new IllegalArgumentException("El asunto es obligatorio");
        if (mensaje == null || mensaje.isBlank()) throw new IllegalArgumentException("El mensaje es obligatorio");
        Objects.requireNonNull(fechaCreacion, "La fecha es obligatoria");
        Objects.requireNonNull(estado, "El estado es obligatorio");
    }

    public Notificacion marcar(EstadoNotificacion nuevoEstado) {
        return new Notificacion(id, idDestinatario, correoDestinatario, tipo, asunto, mensaje, url,
                codigoAcceso, fechaCreacion, Objects.requireNonNull(nuevoEstado));
    }
}
