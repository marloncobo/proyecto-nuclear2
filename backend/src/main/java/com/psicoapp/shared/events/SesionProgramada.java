package com.psicoapp.shared.events;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Un evento por convocado. El codigo es sensible y nunca debe registrarse. */
public record SesionProgramada(UUID idEvento, Instant instante, UUID idEstudiante,
        UUID idSesion, String nombreCaso, String mensaje, URI url, String codigoAcceso)
        implements EventoNotificacion {
    public SesionProgramada {
        Objects.requireNonNull(idEvento);
        Objects.requireNonNull(instante);
        Objects.requireNonNull(idEstudiante);
        Objects.requireNonNull(idSesion);
        if (nombreCaso == null || nombreCaso.isBlank() || codigoAcceso == null || codigoAcceso.isBlank()) {
            throw new IllegalArgumentException("El caso y el codigo son obligatorios");
        }
        if (url == null || url.getHost() == null
                || !("https".equalsIgnoreCase(url.getScheme()) || "http".equalsIgnoreCase(url.getScheme()))
                || url.getUserInfo() != null) {
            throw new IllegalArgumentException("La URL debe ser HTTP o HTTPS sin credenciales");
        }
        mensaje = mensaje == null ? "" : mensaje;
    }

    @Override
    public String toString() {
        return "SesionProgramada[idEvento=" + idEvento + ", datos protegidos]";
    }
}
