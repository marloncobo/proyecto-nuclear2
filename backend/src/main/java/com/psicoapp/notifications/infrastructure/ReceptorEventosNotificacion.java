package com.psicoapp.notifications.infrastructure;

import com.psicoapp.notifications.application.*;
import com.psicoapp.shared.events.*;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
public class ReceptorEventosNotificacion {
    private final List<ObservadorNotificacion> observadores;
    private final RepositorioEntregas entregas;
    public ReceptorEventosNotificacion(List<ObservadorNotificacion> observadores, RepositorioEntregas entregas) {
        this.observadores = List.copyOf(observadores); this.entregas = entregas;
    }
    // Debe participar del commit productor para que no se pierda la solicitud.
    // Solo el despachador realiza la entrega, despues de ser visible el commit.
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void recibir(EventoNotificacion evento) {
        if (evento instanceof ConvocatoriaModificada cambio)
            entregas.cancelarPendientes(cambio.idSesion(), cambio.idEstudiante(),
                    cambio.tipoCambio() == ConvocatoriaModificada.TipoCambio.RETIRO ? "CONVOCATORIA" : "RETIRO");
        for (var observador : observadores) if (observador.soporta(evento))
            observador.actualizar(evento).forEach(entregas::registrar);
    }
}
