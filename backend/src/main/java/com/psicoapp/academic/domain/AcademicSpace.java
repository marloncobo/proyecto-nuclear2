package com.psicoapp.academic.domain;

import java.util.Objects;
import java.util.UUID;

public record AcademicSpace(UUID id, String name, String period) {
    public AcademicSpace {
        Objects.requireNonNull(id, "El identificador es obligatorio");
        if (name == null || name.isBlank() || name.strip().length() > 150) {
            throw new IllegalArgumentException("El nombre es obligatorio y admite hasta 150 caracteres");
        }
        if (period == null || period.isBlank() || period.strip().length() > 30) {
            throw new IllegalArgumentException("El periodo es obligatorio y admite hasta 30 caracteres");
        }
        name = name.strip();
        period = period.strip();
    }
}