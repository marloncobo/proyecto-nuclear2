package com.psicoapp.identity.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioRecuperacionesClave {
    record Recuperacion(String resumen, Instant vence) {
        @Override public String toString() { return "Recuperacion[datos protegidos]"; }
    }
    Optional<Recuperacion> buscar(UUID usuario);
    void guardar(UUID usuario, Recuperacion recuperacion);
    void eliminar(UUID usuario);
}
