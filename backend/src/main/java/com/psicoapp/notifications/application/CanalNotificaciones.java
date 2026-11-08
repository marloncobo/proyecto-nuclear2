package com.psicoapp.notifications.application;

import com.psicoapp.notifications.domain.Notificacion;

public interface CanalNotificaciones {
    void enviar(Notificacion notificacion);
}
