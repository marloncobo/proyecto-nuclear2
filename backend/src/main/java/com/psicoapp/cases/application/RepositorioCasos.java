package com.psicoapp.cases.application;

import com.psicoapp.cases.domain.Caso;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioCasos {
    void guardar(Caso caso);
    Optional<Caso> buscarPorId(UUID id);
    Optional<Caso> bloquearPorId(UUID id);
    void actualizarBorrador(Caso caso);
    boolean archivar(UUID id, UUID idDocente);
    /** Cambia solo BORRADOR del responsable a PUBLICADO; false si ya no coincide. */
    boolean publicar(UUID id, UUID idDocente);
}
