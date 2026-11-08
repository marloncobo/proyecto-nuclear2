package com.psicoapp.shared.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Referencia a la valoracion guardada; no transportar su contenido en el aviso. */
public record RetroalimentacionAgregada(UUID idEvento, Instant instante, UUID idEstudiante,
        UUID idSesion, UUID idIntento, UUID idValoracion) implements EventoNotificacion {
    public RetroalimentacionAgregada {
        Objects.requireNonNull(idEvento);
        Objects.requireNonNull(instante);
        Objects.requireNonNull(idEstudiante);
        Objects.requireNonNull(idSesion);
        Objects.requireNonNull(idIntento);
        Objects.requireNonNull(idValoracion);
    }
}
