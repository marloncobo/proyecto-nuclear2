package com.psicoapp.identity.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record User(UUID id, String email, String name, Role role, String passwordHash,
                   boolean active, Instant createdAt) {
    public User {
        Objects.requireNonNull(id, "El identificador es obligatorio");
        email = normalizeEmail(email);
        if (name == null || name.isBlank() || name.strip().length() > 120) {
            throw new IllegalArgumentException("El nombre es obligatorio y admite hasta 120 caracteres");
        }
        name = name.strip();
        Objects.requireNonNull(role, "El rol es obligatorio");
        Objects.requireNonNull(createdAt, "La fecha de creacion es obligatoria");
        if (role != Role.ESTUDIANTE && (passwordHash == null || passwordHash.isBlank())) {
            throw new IllegalArgumentException("El personal requiere una contrasena");
        }
    }

    public static String normalizeEmail(String email) {
        if (email == null || email.length() > 180 || !email.strip().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Correo invalido");
        }
        return email.strip().toLowerCase(Locale.ROOT);
    }

    public User withRole(Role role) {
        return new User(id, email, name, role, passwordHash, active, createdAt);
    }
}