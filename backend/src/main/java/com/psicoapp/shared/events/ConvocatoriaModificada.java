package com.psicoapp.shared.events;

import java.util.Objects;
import java.util.UUID;
import java.time.Instant;

/** Alta con nuevos datos de acceso; retiro sin codigo ni URL reutilizables. */
public record ConvocatoriaModificada(UUID idEvento, Instant instante, UUID idEstudiante,
        UUID idSesion, TipoCambio tipoCambio, SesionProgramada convocatoria) implements EventoNotificacion {
    public enum TipoCambio { ALTA, RETIRO }

    public ConvocatoriaModificada {
        Objects.requireNonNull(idEvento);
        Objects.requireNonNull(instante);
        Objects.requireNonNull(idEstudiante);
        Objects.requireNonNull(idSesion);
        Objects.requireNonNull(tipoCambio);
        if (tipoCambio == TipoCambio.ALTA && (convocatoria == null
                || !idEstudiante.equals(convocatoria.idEstudiante())
                || !idSesion.equals(convocatoria.idSesion()))) {
            throw new IllegalArgumentException("El alta requiere los datos del mismo convocado y sesion");
        }
        if (tipoCambio == TipoCambio.RETIRO && convocatoria != null) {
            throw new IllegalArgumentException("El retiro no debe incluir credenciales de acceso");
        }
    }
}
