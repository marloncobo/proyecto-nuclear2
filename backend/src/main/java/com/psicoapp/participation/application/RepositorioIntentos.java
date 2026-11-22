package com.psicoapp.participation.application;

import com.psicoapp.participation.domain.Intento;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioIntentos {
    java.util.List<UUID> preguntasDeSesion(UUID idSesion);
    boolean opcionDePregunta(UUID idPregunta, UUID idOpcion);
    void guardar(Intento intento);
    void bloquearEstudiante(UUID idEstudiante);
    boolean existeActivo(UUID idEstudiante);
    boolean tienePermisoReintento(UUID idIntento);
    void registrarPermisoReintento(UUID idIntento, UUID idDocente, java.time.Instant fecha);
    boolean retirarPermisoReintento(UUID idIntento);
    Optional<Intento> buscarPorId(UUID id);
    Optional<Intento> buscarActivo(UUID idSesion, UUID idEstudiante);
    java.util.List<UUID> listarActivosDeSesion(UUID idSesion);
    Optional<Intento> buscarUltimo(UUID idSesion, UUID idEstudiante);
    void actualizar(Intento intento);
    Optional<Intento> bloquearPorId(UUID id);
    java.util.List<Intento> buscarVencidos(java.time.Instant ahora);
}
