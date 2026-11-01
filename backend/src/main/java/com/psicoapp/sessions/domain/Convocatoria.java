package com.psicoapp.sessions.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/** El codigo en claro solo se entrega al docente al generar la convocatoria. */
public record Convocatoria(UUID id, UUID idSesion, UUID idEstudiante, String resumenCodigo, boolean autorizada) {
    public Convocatoria {
        if (id == null || idSesion == null || idEstudiante == null || resumenCodigo == null
                || !resumenCodigo.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Convocatoria o resumen de codigo invalido");
        }
    }

    public static String resumir(String codigo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(codigo.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException excepcion) {
            throw new IllegalStateException(excepcion);
        }
    }
}
