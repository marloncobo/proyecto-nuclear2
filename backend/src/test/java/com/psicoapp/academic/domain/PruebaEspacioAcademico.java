package com.psicoapp.academic.domain;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PruebaEspacioAcademico {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("RF-04 / ER: nombre del espacio obligatorio")
    void exigeNombre(String nombre) {
        assertThrows(IllegalArgumentException.class, () -> new EspacioAcademico(UUID.randomUUID(), nombre, "2026-2"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("RF-04 / ER: periodo obligatorio")
    void exigePeriodo(String periodo) {
        assertThrows(IllegalArgumentException.class, () -> new EspacioAcademico(UUID.randomUUID(), "Psicologia", periodo));
    }

    @ParameterizedTest
    @ValueSource(ints = {151, 200})
    @DisplayName("RF-04 / ER: respeta longitud maxima del nombre")
    void limitaLongitudDelNombre(int longitud) {
        assertThrows(IllegalArgumentException.class, () -> new EspacioAcademico(UUID.randomUUID(), "x".repeat(longitud), "2026-2"));
    }

    @ParameterizedTest
    @ValueSource(ints = {31, 100})
    @DisplayName("RF-04 / ER: respeta longitud maxima del periodo")
    void limitaLongitudDelPeriodo(int longitud) {
        assertThrows(IllegalArgumentException.class, () -> new EspacioAcademico(UUID.randomUUID(), "Psicologia", "x".repeat(longitud)));
    }
}
