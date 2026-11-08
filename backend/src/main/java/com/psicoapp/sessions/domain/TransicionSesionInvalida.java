package com.psicoapp.sessions.domain;

public final class TransicionSesionInvalida extends RuntimeException {
    public TransicionSesionInvalida(String mensaje) {
        super(mensaje);
    }
}
