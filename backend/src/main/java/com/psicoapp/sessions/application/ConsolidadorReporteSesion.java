package com.psicoapp.sessions.application;

import java.time.Instant;
import java.util.UUID;

/** Puerto para conservar en la misma transaccion los resultados derivados del cierre. */
public interface ConsolidadorReporteSesion {
    void consolidar(UUID idSesion, Instant fechaCierre);
}
