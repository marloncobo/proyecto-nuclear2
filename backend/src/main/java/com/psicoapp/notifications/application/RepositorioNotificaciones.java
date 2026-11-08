package com.psicoapp.notifications.application;

import com.psicoapp.notifications.domain.Notificacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioNotificaciones {
    void guardar(Notificacion notificacion);
    void actualizarEstado(UUID id, String estado);
    Optional<Notificacion> buscarPorId(UUID id);
    List<Notificacion> listarPorDestinatario(UUID idDestinatario);
}
