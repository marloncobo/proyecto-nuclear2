package com.psicoapp.shared.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Emitir solo despues de guardar la evaluacion de un intento finalizado. */
public record ResultadosPublicados(UUID idEvento, Instant instante, UUID idEstudiante,
        UUID idSesion, UUID idIntento, BigDecimal calificacion) implements EventoNotificacion {
    public ResultadosPublicados {
        Objects.requireNonNull(idEvento);
        Objects.requireNonNull(instante);
        Objects.requireNonNull(idEstudiante);
        Objects.requireNonNull(idSesion);
        Objects.requireNonNull(idIntento);
        if (calificacion == null || calificacion.signum() < 0) {
            throw new IllegalArgumentException("La calificacion debe ser no negativa");
        }
    }
}
