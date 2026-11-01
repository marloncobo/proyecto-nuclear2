package com.psicoapp.sessions.domain;

import java.time.Instant;

public final class EnCurso implements EstadoSesion {
    public static final EnCurso INSTANCIA = new EnCurso();
    private EnCurso() { }

    @Override public TipoEstadoSesion codigo() { return TipoEstadoSesion.EN_CURSO; }

    @Override
    public Sesion iniciar(Sesion sesion, Instant ahora) {
        throw new TransicionSesionInvalida("La sesion ya esta en curso");
    }

    @Override
    public Sesion finalizar(Sesion sesion, MotivoCierre motivo, Instant ahora) {
        if (motivo == MotivoCierre.AUTOMATICO && !sesion.estaVencida(ahora)) {
            throw new TransicionSesionInvalida("Aun no corresponde el cierre automatico");
        }
        return sesion.conEstado(Finalizado.INSTANCIA);
    }

    @Override
    public void validarParticipacion(Sesion sesion, Instant ahora) {
        if (sesion.estaVencida(ahora)) {
            throw new TransicionSesionInvalida("La sesion alcanzo el limite de cierre automatico");
        }
    }
}
