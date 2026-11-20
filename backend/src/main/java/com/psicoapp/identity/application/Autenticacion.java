package com.psicoapp.identity.application;

import java.time.Instant;

public interface Autenticacion {
    record DatosToken(String token, String tipo, Instant venceEn) {
        @Override public String toString() { return "DatosToken[PROTEGIDO]"; }
    }
    DatosToken ingresar(String correo, String clave);
}
