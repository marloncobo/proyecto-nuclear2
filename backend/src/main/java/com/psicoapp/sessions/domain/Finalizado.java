package com.psicoapp.sessions.domain;

import java.time.Instant;

public final class Finalizado implements EstadoSesion {
    public static final Finalizado INSTANCIA = new Finalizado();
    private Finalizado() { }

    @Override public TipoEstadoSesion codigo() { return TipoEstadoSesion.FINALIZADO; }

    @Override
    public Sesion iniciar(Sesion sesion, Instant ahora) {
        throw new TransicionSesionInvalida("No se puede reiniciar una sesion finalizada");
    }

    @Override
    public Sesion finalizar(Sesion sesion, MotivoCierre motivo, Instant ahora) {
        return sesion;
    }

    @Override
    public void validarParticipacion(Sesion sesion, Instant ahora) {
        throw new TransicionSesionInvalida("No se permite participar en una sesion finalizada");
    }
}
