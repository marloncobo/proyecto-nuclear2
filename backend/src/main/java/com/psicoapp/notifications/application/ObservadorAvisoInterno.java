package com.psicoapp.notifications.application;

import com.psicoapp.shared.events.*;
import com.psicoapp.notifications.domain.SolicitudNotificacion;
import java.util.List;
import static com.psicoapp.notifications.domain.SolicitudNotificacion.Canal.INTERNO;

public class ObservadorAvisoInterno implements ObservadorNotificacion {
    @Override public boolean soporta(EventoNotificacion evento) {
        return evento instanceof ResultadosPublicados || evento instanceof RetroalimentacionAgregada;
    }
    @Override public List<SolicitudNotificacion> actualizar(EventoNotificacion evento) {
        if (evento instanceof ResultadosPublicados resultado) return List.of(new SolicitudNotificacion(
                evento.idEvento(), evento.idEstudiante(), resultado.idSesion(), resultado.idIntento(), INTERNO,
                "RESULTADO", "Calificacion disponible", "Tu calificacion es " + resultado.calificacion().toPlainString()
                        + ". Consulta el resultado y la retroalimentacion de tu intento.", evento.instante()));
        if (evento instanceof RetroalimentacionAgregada valoracion) return List.of(new SolicitudNotificacion(
                evento.idEvento(), evento.idEstudiante(), valoracion.idSesion(), valoracion.idIntento(), INTERNO,
                "VALORACION", "Nueva valoracion docente", "Tu docente agrego una valoracion a tu intento.", evento.instante()));
        return List.of();
    }
}
