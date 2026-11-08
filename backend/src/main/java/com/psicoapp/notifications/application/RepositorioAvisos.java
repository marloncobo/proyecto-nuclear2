package com.psicoapp.notifications.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RepositorioAvisos {
    record Aviso(UUID id, UUID idSesion, UUID idIntento, String tipo, String mensaje,
            Instant fechaCreacion, boolean leida) { }
    List<Aviso> listar(UUID idUsuario, int limite, int desplazamiento);
    boolean marcarLeida(UUID idUsuario, UUID idAviso);
}
