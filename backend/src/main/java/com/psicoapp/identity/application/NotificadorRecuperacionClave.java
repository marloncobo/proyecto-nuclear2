package com.psicoapp.identity.application;

import java.time.Instant;
import java.util.UUID;

public interface NotificadorRecuperacionClave {
    void encolar(UUID usuario, String correo, String token, Instant vence);
}
