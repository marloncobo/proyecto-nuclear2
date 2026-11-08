package com.psicoapp.evaluation.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Contrato publico de seguimiento; oculta persistencia y publicacion de avisos. */
public interface GestionValoraciones {
    record ValoracionPublicada(UUID id, UUID idIntento, UUID idDocente, String mensaje, Instant fecha) { }
    ValoracionPublicada registrar(UUID idDocente, UUID idIntento, String mensaje);
    List<ValoracionPublicada> consultar(UUID idActor, UUID idIntento);
}
