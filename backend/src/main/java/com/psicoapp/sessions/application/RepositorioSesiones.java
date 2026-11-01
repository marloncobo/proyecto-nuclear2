package com.psicoapp.sessions.application;

import com.psicoapp.sessions.domain.Sesion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioSesiones {
    void guardar(Sesion sesion);
    Optional<Sesion> buscarPorId(UUID id);
    /** Requiere transaccion activa; mantiene bloqueo hasta su finalizacion. */
    Optional<Sesion> bloquearPorId(UUID id);
    void actualizarEstado(Sesion sesion);
    List<UUID> buscarVencidas(Instant finLimite);
}
