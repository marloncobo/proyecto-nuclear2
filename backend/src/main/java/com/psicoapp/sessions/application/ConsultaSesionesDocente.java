package com.psicoapp.sessions.application;

import java.util.UUID;

/** Consulta autorizada sin exponer entidades ni convocatorias. */
public interface ConsultaSesionesDocente {
    void exigirResponsable(UUID idDocente, UUID idSesion);
}
