package com.psicoapp.notifications.application;

import com.psicoapp.notifications.domain.SolicitudNotificacion;
import java.time.Instant;
import java.util.*;

public interface RepositorioEntregas {
    record Entrega(UUID id, UUID idUsuario, UUID idSesion, String canal, String tipo,
            String asunto, String contenido, int intentos) {
        @Override public String toString() { return "Entrega[id=" + id + "]"; }
    }
    void registrar(SolicitudNotificacion solicitud);
    List<UUID> pendientes(Instant ahora, int limite);
    Optional<Entrega> bloquearPendiente(UUID id, Instant ahora);
    String recuperarContenido(Entrega entrega);
    void confirmar(UUID id, Instant ahora);
    void posponer(UUID id, int intentos, Instant siguiente);
    void cancelarPendientes(UUID idSesion, UUID idEstudiante, String tipo);
}
