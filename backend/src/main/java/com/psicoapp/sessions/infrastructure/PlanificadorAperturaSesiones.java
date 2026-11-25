package com.psicoapp.sessions.infrastructure;

import com.psicoapp.sessions.application.ServicioGestionSesiones;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "psicoapp.sesiones.apertura-automatica-habilitada", havingValue = "true", matchIfMissing = true)
public class PlanificadorAperturaSesiones {
    private final ServicioGestionSesiones sesiones;
    private final Clock reloj;

    public PlanificadorAperturaSesiones(ServicioGestionSesiones sesiones, Clock reloj) {
        this.sesiones = sesiones;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${psicoapp.sesiones.intervalo-apertura-ms:5000}",
            initialDelayString = "${psicoapp.sesiones.intervalo-apertura-ms:5000}")
    public void abrirProgramadas() {
        sesiones.abrirProgramadas(reloj.instant());
    }
}
