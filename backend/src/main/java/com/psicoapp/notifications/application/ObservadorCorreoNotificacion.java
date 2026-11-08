package com.psicoapp.notifications.application;

import com.psicoapp.shared.events.*;
import com.psicoapp.notifications.domain.SolicitudNotificacion;
import java.util.List;
import static com.psicoapp.notifications.domain.SolicitudNotificacion.Canal.CORREO;

public class ObservadorCorreoNotificacion implements ObservadorNotificacion {
    @Override public boolean soporta(EventoNotificacion evento) {
        return evento instanceof SesionProgramada || evento instanceof ConvocatoriaModificada;
    }
    @Override public List<SolicitudNotificacion> actualizar(EventoNotificacion evento) {
        if (evento instanceof SesionProgramada sesion) return List.of(convocatoria(evento, sesion));
        if (evento instanceof ConvocatoriaModificada cambio) {
            if (cambio.tipoCambio() == ConvocatoriaModificada.TipoCambio.ALTA)
                return List.of(convocatoria(evento, cambio.convocatoria()));
            return List.of(new SolicitudNotificacion(evento.idEvento(), evento.idEstudiante(),
                    cambio.idSesion(), null, CORREO, "RETIRO", "Retiro de convocatoria",
                    "Has sido retirado de la convocatoria de la sesion " + cambio.idSesion()
                            + ". Su codigo de acceso ya no autoriza esta participacion.", evento.instante()));
        }
        return List.of();
    }
    private SolicitudNotificacion convocatoria(EventoNotificacion evento, SesionProgramada sesion) {
        return new SolicitudNotificacion(evento.idEvento(), evento.idEstudiante(), sesion.idSesion(), null,
                CORREO, "CONVOCATORIA", "Convocatoria academica", "Caso: " + sesion.nombreCaso()
                + "\n" + sesion.mensaje() + "\nAcceso: " + sesion.url().toASCIIString()
                + "\nCodigo: " + sesion.codigoAcceso(), evento.instante());
    }
}
