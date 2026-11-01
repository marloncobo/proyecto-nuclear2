package com.psicoapp.participation.application;

import com.psicoapp.participation.domain.EstadoIntento;
import com.psicoapp.participation.domain.Intento;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioIntentos {
    void guardar(Intento intento);
    Optional<Intento> buscarPorId(UUID id);
    Optional<Intento> buscarActivo(UUID idSesion, UUID idEstudiante);
    void actualizar(Intento intento);
    Optional<Intento> bloquearPorId(UUID id);
    java.util.List<Intento> buscarVencidos(java.time.Instant ahora);
}
