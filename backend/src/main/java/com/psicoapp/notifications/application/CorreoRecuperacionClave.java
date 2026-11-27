package com.psicoapp.notifications.application;

import com.psicoapp.identity.application.NotificadorRecuperacionClave;
import com.psicoapp.notifications.domain.SolicitudNotificacion;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import static com.psicoapp.notifications.domain.SolicitudNotificacion.Canal.CORREO;

@Component
public class CorreoRecuperacionClave implements NotificadorRecuperacionClave {
    private static final String TIPO = "RECUPERACION_CLAVE";
    private final RepositorioEntregas entregas;
    private final Clock reloj;
    private final String urlAcceso;

    public CorreoRecuperacionClave(RepositorioEntregas entregas, Clock reloj,
            @Value("${psicoapp.notificaciones.url-acceso:http://localhost:3000/ingreso}") String urlAcceso) {
        this.entregas = entregas;
        this.reloj = reloj;
        this.urlAcceso = urlAcceso;
    }

    @Override
    public void encolar(UUID usuario, String correo, String token, Instant vence) {
        String enlace = UriComponentsBuilder.fromUriString(urlAcceso).fragment("recuperar=" + token)
                .build(true).toUriString();
        String titulo = "Restablece tu contraseña";
        var contenido = PlantillaCorreo.preparar(titulo,
                "Recibimos una solicitud para cambiar la contraseña de tu cuenta de Psicoapp.",
                null, List.of(), null, enlace, "Elegir nueva contraseña",
                "El enlace vence en 30 minutos y solo puede usarse una vez. Si no solicitaste el cambio, puedes ignorar este correo.");
        entregas.cancelarCorreoPendiente(usuario, TIPO);
        entregas.registrar(new SolicitudNotificacion(UUID.randomUUID(), usuario, null, null, CORREO,
                TIPO, titulo, contenido.texto(), reloj.instant(), contenido.html(), correo, vence));
    }
}
