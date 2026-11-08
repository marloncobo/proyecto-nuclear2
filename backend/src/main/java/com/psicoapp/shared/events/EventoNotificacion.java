package com.psicoapp.shared.events;

import java.time.Instant;
import java.util.UUID;

/** Datos minimos compartidos; nunca incluir entidades ni claves de usuario. */
public interface EventoNotificacion {
    UUID idEvento();
    Instant instante();
    UUID idEstudiante();
}
