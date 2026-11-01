package com.psicoapp.sessions.infrastructure;

import com.psicoapp.sessions.application.ServicioGestionSesiones;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "psicoapp.sesiones.cierre-automatico-habilitado", havingValue = "true", matchIfMissing = true)
public class PlanificadorCierreSesiones {
    private final ServicioGestionSesiones sesiones;
    private final Clock reloj;

    public PlanificadorCierreSesiones(ServicioGestionSesiones sesiones, Clock reloj) {
        this.sesiones = sesiones;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${psicoapp.sesiones.intervalo-cierre-ms:60000}",
            initialDelayString = "${psicoapp.sesiones.intervalo-cierre-ms:60000}")
    public void cerrarVencidas() {
        sesiones.cerrarVencidas(reloj.instant());
    }
}
