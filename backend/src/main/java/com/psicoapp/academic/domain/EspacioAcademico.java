package com.psicoapp.academic.domain;

import java.util.Objects;
import java.util.UUID;

public record EspacioAcademico(UUID id, String nombre, String periodo) {
    public EspacioAcademico {
        Objects.requireNonNull(id, "El identificador es obligatorio");
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 150) {
            throw new IllegalArgumentException("El nombre es obligatorio y admite hasta 150 caracteres");
        }
        if (periodo == null || periodo.isBlank() || periodo.strip().length() > 30) {
            throw new IllegalArgumentException("El periodo es obligatorio y admite hasta 30 caracteres");
        }
        nombre = nombre.strip();
        periodo = periodo.strip();
    }
}
