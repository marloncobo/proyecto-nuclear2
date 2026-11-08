package com.psicoapp.sessions;

import com.psicoapp.sessions.domain.*;
import java.time.Instant;
import java.util.UUID;

public final class DatosPruebaSesiones {
    public static final Instant INICIO = Instant.parse("2026-11-01T14:00:00Z");
    public static final Instant FIN = Instant.parse("2026-11-01T15:00:00Z");
    private DatosPruebaSesiones() { }

    public static Sesion programada(UUID docente) {
        return Sesion.programar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), docente,
                INICIO, FIN, 30, 40, ModoNavegacion.FLEXIBLE, null, null);
    }
}
