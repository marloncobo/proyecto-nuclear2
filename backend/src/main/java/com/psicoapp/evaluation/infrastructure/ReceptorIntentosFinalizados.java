package com.psicoapp.evaluation.infrastructure;

import com.psicoapp.evaluation.application.ServicioEvaluacion;
import com.psicoapp.shared.events.IntentoFinalizado;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
public class ReceptorIntentosFinalizados {
    private final ServicioEvaluacion evaluaciones;
    public ReceptorIntentosFinalizados(ServicioEvaluacion evaluaciones) { this.evaluaciones = evaluaciones; }
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void evaluar(IntentoFinalizado evento) {
        evaluaciones.evaluar(evento.idEstudiante(), evento.idIntento());
    }
}
