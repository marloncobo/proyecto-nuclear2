package com.psicoapp.notifications.application;

import com.psicoapp.shared.events.*;
import com.psicoapp.notifications.domain.SolicitudNotificacion;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import static com.psicoapp.notifications.domain.SolicitudNotificacion.Canal.INTERNO;

public class ObservadorAvisoInterno implements ObservadorNotificacion {
    private static final DateTimeFormatter HORARIO_COLOMBIA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu 'a las' HH:mm").withZone(ZoneId.of("America/Bogota"));

    @Override public boolean soporta(EventoNotificacion evento) {
        return evento instanceof SesionProgramada || evento instanceof ConvocatoriaModificada cambio
                && cambio.tipoCambio() == ConvocatoriaModificada.TipoCambio.ALTA
                || evento instanceof ResultadosPublicados || evento instanceof RetroalimentacionAgregada;
    }
    @Override public List<SolicitudNotificacion> actualizar(EventoNotificacion evento) {
        if (evento instanceof SesionProgramada sesion) return List.of(convocatoria(evento, sesion));
        if (evento instanceof ConvocatoriaModificada cambio
                && cambio.tipoCambio() == ConvocatoriaModificada.TipoCambio.ALTA) {
            return List.of(convocatoria(evento, cambio.convocatoria()));
        }
        if (evento instanceof ResultadosPublicados resultado) return List.of(new SolicitudNotificacion(
                evento.idEvento(), evento.idEstudiante(), resultado.idSesion(), resultado.idIntento(), INTERNO,
                "RESULTADO", "Calificacion disponible", "Tu calificacion es " + resultado.calificacion().toPlainString()
                        + ". Consulta el resultado y la retroalimentacion de tu intento.", evento.instante()));
        if (evento instanceof RetroalimentacionAgregada valoracion) return List.of(new SolicitudNotificacion(
                evento.idEvento(), evento.idEstudiante(), valoracion.idSesion(), valoracion.idIntento(), INTERNO,
                "VALORACION", "Nueva valoracion docente", "Tu docente agrego una valoracion a tu intento.", evento.instante()));
        return List.of();
    }

    private SolicitudNotificacion convocatoria(EventoNotificacion evento, SesionProgramada sesion) {
        return new SolicitudNotificacion(evento.idEvento(), evento.idEstudiante(), sesion.idSesion(), null,
                INTERNO, "CONVOCATORIA", "Nueva convocatoria",
                "Te convocaron a la sesión «" + sesion.nombreCaso() + "». Apertura programada: "
                        + HORARIO_COLOMBIA.format(sesion.inicioPrevisto()) + " (hora de Colombia).",
                evento.instante());
    }
}
