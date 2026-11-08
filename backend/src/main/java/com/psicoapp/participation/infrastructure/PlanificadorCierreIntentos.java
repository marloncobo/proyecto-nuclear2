package com.psicoapp.participation.infrastructure;

import com.psicoapp.participation.application.ServicioGestionParticipacion;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "psicoapp.participacion.cierre-automatico-habilitado",
        havingValue = "true", matchIfMissing = true)
public class PlanificadorCierreIntentos {
    private final ServicioGestionParticipacion participaciones;
    private final Clock reloj;

    public PlanificadorCierreIntentos(ServicioGestionParticipacion participaciones, Clock reloj) {
        this.participaciones = participaciones;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${psicoapp.participacion.intervalo-cierre-ms:60000}",
            initialDelayString = "${psicoapp.participacion.intervalo-cierre-ms:60000}")
    public void cerrarVencidos() {
        participaciones.finalizarVencidos(reloj.instant());
    }
}
