package com.psicoapp.notifications.infrastructure;

import com.psicoapp.notifications.application.*;
import com.psicoapp.identity.application.DirectorioIdentidad;
import java.time.Clock;
import org.springframework.context.annotation.*;

@Configuration
public class ConfiguracionNotificaciones {
    @Bean ObservadorCorreoNotificacion observadorCorreo() { return new ObservadorCorreoNotificacion(); }
    @Bean ObservadorAvisoInterno observadorAvisoInterno() { return new ObservadorAvisoInterno(); }
    @Bean ServicioEntregaNotificaciones servicioEntregaNotificaciones(RepositorioEntregas entregas,
            PuertoCorreo correo, DirectorioIdentidad identidades, Clock reloj) {
        return new ServicioEntregaNotificaciones(entregas, correo, identidades, reloj);
    }
}
