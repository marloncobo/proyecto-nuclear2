package com.psicoapp.sessions.application;

import java.util.UUID;

/** Ingreso inicial: el consumidor debe mantener esta misma transaccion al crear el intento. */
public interface AutorizacionConvocatorias {
    void exigirAcceso(UUID estudiante, UUID sesion, String codigo);
}
