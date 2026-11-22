package com.psicoapp.notifications.infrastructure;

import com.psicoapp.notifications.application.GeneradorAvisosDocente;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "psicoapp.notificaciones.avisos-docente-habilitados",
        havingValue = "true", matchIfMissing = true)
public class PlanificadorAvisosDocente {
    private final GeneradorAvisosDocente avisos;
    private final Clock reloj;
    private final Duration anticipacion;

    public PlanificadorAvisosDocente(GeneradorAvisosDocente avisos, Clock reloj,
            @Value("${psicoapp.notificaciones.recordatorio-horas:24}") long horas) {
        if (horas < 1 || horas > 168) throw new IllegalArgumentException("Anticipacion de recordatorio invalida");
        this.avisos = avisos;
        this.reloj = reloj;
        this.anticipacion = Duration.ofHours(horas);
    }

    @Scheduled(fixedDelayString = "${psicoapp.notificaciones.avisos-docente-intervalo-ms:60000}",
            initialDelayString = "${psicoapp.notificaciones.avisos-docente-demora-inicial-ms:1000}")
    public void generar() {
        avisos.generar(reloj.instant(), anticipacion);
    }
}
