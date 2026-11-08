package com.psicoapp.notifications.infrastructure;

import com.psicoapp.notifications.application.CanalNotificaciones;
import com.psicoapp.notifications.domain.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida inicial. La entrega queda registrada y el canal se
 * puede sustituir por SMTP sin modificar los casos de uso.
 */
@Component
public class CanalCorreoConfigurado implements CanalNotificaciones {
    private static final Logger LOG = LoggerFactory.getLogger(CanalCorreoConfigurado.class);

    @Override
    public void enviar(Notificacion notificacion) {
        LOG.info("Notificacion {} preparada para {}", notificacion.id(), notificacion.correoDestinatario());
    }
}
