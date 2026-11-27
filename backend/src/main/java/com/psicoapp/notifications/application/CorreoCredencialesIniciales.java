package com.psicoapp.notifications.application;

import com.psicoapp.identity.application.NotificadorCredencialesIniciales;
import com.psicoapp.notifications.domain.SolicitudNotificacion;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.psicoapp.notifications.domain.SolicitudNotificacion.Canal.CORREO;

@Component
public class CorreoCredencialesIniciales implements NotificadorCredencialesIniciales {
    private final RepositorioEntregas entregas;
    private final Clock reloj;
    private final String urlAcceso;

    public CorreoCredencialesIniciales(RepositorioEntregas entregas, Clock reloj,
            @Value("${psicoapp.notificaciones.url-acceso:http://localhost:3000/ingreso}") String urlAcceso) {
        this.entregas = entregas;
        this.reloj = reloj;
        this.urlAcceso = urlAcceso;
    }

    @Override
    public void encolar(UUID idUsuario, String correo, String clave) {
        String titulo = "Tu acceso a Psicoapp";
        var contenido = PlantillaCorreo.preparar(titulo, "Tu cuenta de Psicoapp está lista.", "Datos de acceso",
                List.of(new PlantillaCorreo.Dato("Usuario", correo), new PlantillaCorreo.Dato("Contraseña inicial", clave)),
                null, urlAcceso, "Ingresar a Psicoapp", "Cambia tu contraseña después de iniciar sesión y evita compartirla.");
        entregas.registrar(new SolicitudNotificacion(idUsuario, idUsuario, null, null, CORREO,
                "CREDENCIALES_INICIALES", titulo, contenido.texto(), reloj.instant(), contenido.html(), correo));
    }
}
