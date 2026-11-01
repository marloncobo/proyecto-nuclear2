package com.psicoapp.sessions.domain;

import java.time.Instant;

/** State: cada estado decide las operaciones permitidas sin HTTP, SQL ni Spring. */
public sealed interface EstadoSesion permits SinIniciar, EnCurso, Finalizado {
    TipoEstadoSesion codigo();
    Sesion iniciar(Sesion sesion, Instant ahora);
    Sesion finalizar(Sesion sesion, MotivoCierre motivo, Instant ahora);
    void validarParticipacion(Sesion sesion, Instant ahora);

    static EstadoSesion desde(TipoEstadoSesion codigo) {
        if (codigo == null) throw new IllegalArgumentException("El estado es obligatorio");
        return switch (codigo) {
            case SIN_INICIAR -> SinIniciar.INSTANCIA;
            case EN_CURSO -> EnCurso.INSTANCIA;
            case FINALIZADO -> Finalizado.INSTANCIA;
        };
    }
}
