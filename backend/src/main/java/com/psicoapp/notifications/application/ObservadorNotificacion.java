package com.psicoapp.notifications.application;

import com.psicoapp.shared.events.EventoNotificacion;
import com.psicoapp.notifications.domain.SolicitudNotificacion;
import java.util.List;

public interface ObservadorNotificacion {
    boolean soporta(EventoNotificacion evento);
    List<SolicitudNotificacion> actualizar(EventoNotificacion evento);
}
