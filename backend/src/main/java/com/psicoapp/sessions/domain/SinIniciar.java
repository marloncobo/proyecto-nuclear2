package com.psicoapp.sessions.domain;

import java.time.Instant;

public final class SinIniciar implements EstadoSesion {
    public static final SinIniciar INSTANCIA = new SinIniciar();
    private SinIniciar() { }

    @Override public TipoEstadoSesion codigo() { return TipoEstadoSesion.SIN_INICIAR; }

    @Override
    public Sesion iniciar(Sesion sesion, Instant ahora) {
        if (ahora.isBefore(sesion.inicioPrevisto())) {
            throw new TransicionSesionInvalida("Aun no se alcanza el inicio previsto");
        }
        if (sesion.estaVencida(ahora)) {
            throw new TransicionSesionInvalida("La sesion ya alcanzo su cierre automatico");
        }
        return sesion.conEstado(EnCurso.INSTANCIA);
    }

    @Override
    public Sesion finalizar(Sesion sesion, MotivoCierre motivo, Instant ahora) {
        if (motivo != MotivoCierre.AUTOMATICO || !sesion.estaVencida(ahora)) {
            throw new TransicionSesionInvalida("Una sesion sin iniciar solo admite cierre automatico al vencer");
        }
        return sesion.conEstado(Finalizado.INSTANCIA);
    }

    @Override
    public void validarParticipacion(Sesion sesion, Instant ahora) {
        throw new TransicionSesionInvalida("La sesion aun no ha sido iniciada");
    }
}
