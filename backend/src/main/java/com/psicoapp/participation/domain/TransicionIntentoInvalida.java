package com.psicoapp.participation.domain;

public class TransicionIntentoInvalida extends RuntimeException {
    public TransicionIntentoInvalida(String mensaje) {
        super(mensaje);
    }
}
