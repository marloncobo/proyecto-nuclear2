package com.psicoapp.sessions.application;

import java.util.UUID;

/** Ingreso inicial: el consumidor debe mantener esta misma transaccion al crear el intento. */
public interface AutorizacionConvocatorias {
    default void exigirAcceso(UUID estudiante, UUID sesion) {
        exigirAcceso(estudiante, sesion, null);
    }
    void exigirAcceso(UUID estudiante, UUID sesion, String codigoIgnorado);
    void exigirParticipacionVigente(UUID estudiante, UUID sesion);
}
