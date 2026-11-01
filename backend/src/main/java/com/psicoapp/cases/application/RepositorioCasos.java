package com.psicoapp.cases.application;

import com.psicoapp.cases.domain.Caso;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioCasos {
    void guardar(Caso caso);
    Optional<Caso> buscarPorId(UUID id);
    /** Cambia solo BORRADOR del responsable a PUBLICADO; false si ya no coincide. */
    boolean publicar(UUID id, UUID idDocente);
}
