package com.psicoapp.shared.configuration;

import com.psicoapp.shared.events.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** Publica a observadores que registran la outbox; no realiza E/S de red. */
@Component
public class SpringNotificacionSujeto implements NotificacionSujeto {
    private final ApplicationEventPublisher eventos;
    public SpringNotificacionSujeto(ApplicationEventPublisher eventos) { this.eventos = eventos; }
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publicar(EventoNotificacion evento) { eventos.publishEvent(evento); }
}
