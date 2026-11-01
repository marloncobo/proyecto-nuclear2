package com.psicoapp.sessions.application;

import java.time.Instant;
import java.util.UUID;

/** Integracion interna. No autentica ni autoriza estudiantes o convocatorias. */
public interface ConsultaSesiones {
    record SesionEnCurso(UUID id, UUID idCaso, Instant inicioPrevisto, Instant finPrevisto,
            int duracionMaximaMin, String modoNavegacion) { }

    /**
     * El consumidor debe invocar dentro de la transaccion que registra la participacion.
     * El bloqueo se conserva hasta terminar esa transaccion; no sustituye otras reglas de ingreso.
     */
    SesionEnCurso exigirEnCurso(UUID idSesion);
}
