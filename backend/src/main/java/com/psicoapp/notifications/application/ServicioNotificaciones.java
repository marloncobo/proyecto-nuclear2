package com.psicoapp.notifications.application;

import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.notifications.domain.EstadoNotificacion;
import com.psicoapp.notifications.domain.Notificacion;
import com.psicoapp.notifications.domain.TipoNotificacion;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioNotificaciones {
    public record DatosNotificacion(UUID id, UUID idDestinatario, String correoDestinatario,
            String tipo, String asunto, String mensaje, String url, String codigoAcceso,
            java.time.Instant fechaCreacion, String estado) { }

    private final RepositorioNotificaciones repositorio;
    private final CanalNotificaciones canal;
    private final DirectorioIdentidad identidades;
    private final Clock reloj;

    public ServicioNotificaciones(RepositorioNotificaciones repositorio, CanalNotificaciones canal,
            DirectorioIdentidad identidades, Clock reloj) {
        this.repositorio = repositorio;
        this.canal = canal;
        this.identidades = identidades;
        this.reloj = reloj;
    }

    public DatosNotificacion crearConvocatoria(UUID idDestinatario, String correo, String asunto,
            String mensaje, String url, String codigoAcceso) {
        return crear(idDestinatario, correo, TipoNotificacion.CONVOCATORIA, asunto, mensaje, url, codigoAcceso);
    }

    public DatosNotificacion crearResultado(UUID idDestinatario, String correo, String asunto, String mensaje,
            String url) {
        return crear(idDestinatario, correo, TipoNotificacion.RESULTADO, asunto, mensaje, url, null);
    }

    public DatosNotificacion crearRetroalimentacion(UUID idDestinatario, String correo, String asunto,
            String mensaje, String url) {
        return crear(idDestinatario, correo, TipoNotificacion.RETROALIMENTACION, asunto, mensaje, url, null);
    }

    @Transactional(readOnly = true)
    public List<DatosNotificacion> listar(UUID idSolicitante, UUID idDestinatario) {
        if (!idSolicitante.equals(idDestinatario)) {
            throw new com.psicoapp.shared.exceptions.AccesoDenegado(
                    "Solo el destinatario puede consultar sus notificaciones");
        }
        identidades.exigirEstudiante(idSolicitante);
        return repositorio.listarPorDestinatario(idDestinatario).stream().map(this::representar).toList();
    }

    private DatosNotificacion crear(UUID idDestinatario, String correo, TipoNotificacion tipo, String asunto,
            String mensaje, String url, String codigoAcceso) {
        Notificacion notificacion = new Notificacion(UUID.randomUUID(), idDestinatario, correo, tipo, asunto,
                mensaje, url, codigoAcceso, reloj.instant(), EstadoNotificacion.PENDIENTE);
        repositorio.guardar(notificacion);
        try {
            canal.enviar(notificacion);
            repositorio.actualizarEstado(notificacion.id(), EstadoNotificacion.ENVIADA.name());
            return representar(notificacion.marcar(EstadoNotificacion.ENVIADA));
        } catch (RuntimeException excepcion) {
            repositorio.actualizarEstado(notificacion.id(), EstadoNotificacion.FALLIDA.name());
            throw excepcion;
        }
    }

    private DatosNotificacion representar(Notificacion notificacion) {
        return new DatosNotificacion(notificacion.id(), notificacion.idDestinatario(),
                notificacion.correoDestinatario(), notificacion.tipo().name(), notificacion.asunto(),
                notificacion.mensaje(), notificacion.url(), notificacion.codigoAcceso(),
                notificacion.fechaCreacion(), notificacion.estado().name());
    }
}
