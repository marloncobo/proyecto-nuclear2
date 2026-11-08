package com.psicoapp.shared.events;

/** La implementacion debe conservar la solicitud en la transaccion productora. */
public interface NotificacionSujeto {
    void publicar(EventoNotificacion evento);
}
