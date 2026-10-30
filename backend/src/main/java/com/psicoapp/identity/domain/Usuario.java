package com.psicoapp.identity.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record Usuario(UUID id, String correo, String nombre, Rol rol, String hashClave,
                   boolean activo, Instant fechaCreacion) {
    public Usuario {
        Objects.requireNonNull(id, "El identificador es obligatorio");
        correo = normalizarCorreo(correo);
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 120) {
            throw new IllegalArgumentException("El nombre es obligatorio y admite hasta 120 caracteres");
        }
        nombre = nombre.strip();
        Objects.requireNonNull(rol, "El rol es obligatorio");
        Objects.requireNonNull(fechaCreacion, "La fecha de creacion es obligatoria");
        if (rol != Rol.ESTUDIANTE && (hashClave == null || hashClave.isBlank())) {
            throw new IllegalArgumentException("El personal requiere una contrasena");
        }
    }

    public static String normalizarCorreo(String correo) {
        if (correo == null || correo.length() > 180 || !correo.strip().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Correo invalido");
        }
        return correo.strip().toLowerCase(Locale.ROOT);
    }

    public Usuario conRol(Rol rol) {
        return new Usuario(id, correo, nombre, rol, hashClave, activo, fechaCreacion);
    }
}
