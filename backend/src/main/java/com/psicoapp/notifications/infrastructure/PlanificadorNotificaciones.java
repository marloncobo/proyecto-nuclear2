package com.psicoapp.notifications.infrastructure;

import com.psicoapp.notifications.application.ServicioEntregaNotificaciones;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "psicoapp.notificaciones.despacho-habilitado", havingValue = "true", matchIfMissing = true)
public class PlanificadorNotificaciones {
    private final ServicioEntregaNotificaciones entregas;
    public PlanificadorNotificaciones(ServicioEntregaNotificaciones entregas) { this.entregas = entregas; }
    @Scheduled(fixedDelayString = "${psicoapp.notificaciones.intervalo-ms:30000}",
            initialDelayString = "${psicoapp.notificaciones.intervalo-ms:30000}")
    public void despachar() {
        for (var id : entregas.pendientes()) entregas.entregar(id);
    }
}
