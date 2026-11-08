package com.psicoapp.cases.application;

import java.util.UUID;

/** Contrato publico para programacion; no expone respuestas correctas ni entidades internas. */
public interface CatalogoCasos {
    record CasoProgramable(UUID id, int tiempoEstimadoMin, String nombre) { }
    String consultarNombre(UUID idDocente, UUID idCaso);
    CasoProgramable exigirPublicado(UUID idDocente, UUID idCaso);
}
