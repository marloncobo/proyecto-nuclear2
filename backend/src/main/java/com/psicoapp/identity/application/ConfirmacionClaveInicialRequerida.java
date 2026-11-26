package com.psicoapp.identity.application;

public class ConfirmacionClaveInicialRequerida extends RuntimeException {
    public ConfirmacionClaveInicialRequerida() {
        super("Confirma si quieres conservar tu contraseña inicial");
    }
}
