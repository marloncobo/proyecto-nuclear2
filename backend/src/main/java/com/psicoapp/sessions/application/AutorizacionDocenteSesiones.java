package com.psicoapp.sessions.application;

import java.util.UUID;

public interface AutorizacionDocenteSesiones {
    void exigirResponsable(UUID idDocente, UUID idSesion);
}
