package com.psicoapp.notifications.application;

import java.time.Clock;
import java.util.UUID;
import java.util.List;
import com.psicoapp.identity.application.DirectorioIdentidad;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/** Una transaccion por entrega y bloqueo hasta confirmar: permite varios trabajadores. */
public class ServicioEntregaNotificaciones {
    private final RepositorioEntregas entregas;
    private final PuertoCorreo correo;
    private final DirectorioIdentidad identidades;
    private final Clock reloj;
    public ServicioEntregaNotificaciones(RepositorioEntregas entregas, PuertoCorreo correo,
            DirectorioIdentidad identidades, Clock reloj) {
        this.entregas = entregas; this.correo = correo; this.identidades = identidades; this.reloj = reloj;
    }
    public List<UUID> pendientes() { return entregas.pendientes(reloj.instant(), 100); }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void entregar(UUID id) {
        var ahora = reloj.instant();
        var pendiente = entregas.bloquearPendiente(id, ahora);
        if (pendiente.isEmpty()) return;
        var entrega = pendiente.get();
        if ("CORREO".equals(entrega.canal())) {
            try {
                String destinatario = identidades.consultarIdentidad(entrega.idUsuario()).correo();
                correo.enviar(destinatario, entrega.asunto(), entregas.recuperarContenido(entrega));
            } catch (RuntimeException excepcion) {
                // No conservar mensaje/stack del proveedor: pueden contener codigo o destinatario.
                int intentos = Math.min(entrega.intentos() + 1, 1000000);
                long espera = Math.min(3600L, 30L * (1L << Math.min(intentos - 1, 7)));
                entregas.posponer(id, intentos, ahora.plusSeconds(espera));
                return;
            }
        }
        entregas.confirmar(id, ahora);
    }
}
