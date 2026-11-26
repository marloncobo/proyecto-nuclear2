package com.psicoapp.notifications.application;

import com.psicoapp.shared.events.*;
import com.psicoapp.notifications.domain.ContenidoCorreo;
import com.psicoapp.notifications.domain.SolicitudNotificacion;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.web.util.UriComponentsBuilder;
import com.psicoapp.notifications.application.PlantillaCorreo.Dato;
import static com.psicoapp.notifications.domain.SolicitudNotificacion.Canal.CORREO;

public class ObservadorCorreoNotificacion implements ObservadorNotificacion {
    private static final DateTimeFormatter HORARIO_COLOMBIA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu, HH:mm").withZone(ZoneId.of("America/Bogota"));

    @Override public boolean soporta(EventoNotificacion evento) {
        return evento instanceof SesionProgramada || evento instanceof ConvocatoriaModificada;
    }
    @Override public List<SolicitudNotificacion> actualizar(EventoNotificacion evento) {
        if (evento instanceof SesionProgramada sesion) return List.of(convocatoria(evento, sesion));
        if (evento instanceof ConvocatoriaModificada cambio) {
            if (cambio.tipoCambio() == ConvocatoriaModificada.TipoCambio.ALTA)
                return List.of(convocatoria(evento, cambio.convocatoria()));
            String titulo = "Retiro de convocatoria";
            var contenido = PlantillaCorreo.preparar(titulo,
                    "Tu convocatoria fue retirada. Ya no puedes iniciar ni continuar esta actividad.",
                    List.of(new Dato("Caso", cambio.nombreCaso()),
                            new Dato("Referencia de la sesión", cambio.idSesion().toString())), null, null);
            return List.of(solicitud(evento, cambio.idSesion(), "RETIRO", titulo, contenido));
        }
        return List.of();
    }
    private SolicitudNotificacion convocatoria(EventoNotificacion evento, SesionProgramada sesion) {
        var enlace = UriComponentsBuilder.fromUri(sesion.url())
                .replaceQueryParam("sesion", sesion.idSesion())
                .build(true).toUri().toASCIIString();
        var datos = new ArrayList<>(List.of(
                new Dato("Caso", sesion.nombreCaso()),
                new Dato("Apertura (hora de Colombia)", HORARIO_COLOMBIA.format(sesion.inicioPrevisto())),
                new Dato("Cierre (hora de Colombia)", HORARIO_COLOMBIA.format(sesion.finPrevisto())),
                new Dato("Tiempo máximo para resolver", sesion.duracionMaximaMin() + " min")));
        if (sesion.lugar() != null && !sesion.lugar().isBlank()) datos.add(new Dato("Lugar", sesion.lugar()));

        String titulo = "Convocatoria académica";
        var contenido = PlantillaCorreo.preparar(titulo, "Te convocaron a una actividad académica en Psicoapp.",
                datos, sesion.mensaje(), enlace);
        return solicitud(evento, sesion.idSesion(), "CONVOCATORIA", titulo, contenido);
    }

    private SolicitudNotificacion solicitud(EventoNotificacion evento, UUID sesion, String tipo,
            String titulo, ContenidoCorreo contenido) {
        return new SolicitudNotificacion(evento.idEvento(), evento.idEstudiante(), sesion, null,
                CORREO, tipo, titulo, contenido.texto(), evento.instante(), contenido.html());
    }
}
