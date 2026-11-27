package com.psicoapp.identity.application;

import java.util.UUID;

/** Encola el correo de alta dentro de la transacción de creación de la cuenta. */
public interface NotificadorCredencialesIniciales {
    void encolar(UUID idUsuario, String correo, String clave);
}
