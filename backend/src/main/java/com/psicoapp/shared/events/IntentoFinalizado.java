package com.psicoapp.shared.events;

import java.time.Instant;
import java.util.UUID;
import java.util.Objects;

/** Solicita evaluacion transaccional sin acoplar participation con evaluation. */
public record IntentoFinalizado(UUID idEvento, Instant instante, UUID idEstudiante, UUID idIntento)
        implements EventoNotificacion {
    public IntentoFinalizado {
        Objects.requireNonNull(idEvento); Objects.requireNonNull(instante);
        Objects.requireNonNull(idEstudiante); Objects.requireNonNull(idIntento);
    }
}
