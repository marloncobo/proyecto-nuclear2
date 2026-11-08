package com.psicoapp.cases.application;

import java.util.UUID;

/** Autoriza el seguimiento de un caso propio, incluso si esta archivado. */
public interface ConsultaCasosDocente {
    void exigirResponsable(UUID idDocente, UUID idCaso);
}
