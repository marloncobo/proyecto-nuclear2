package com.psicoapp.academic.domain;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AcademicSpaceTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("RF-04 / ER: nombre del espacio obligatorio")
    void requiresName(String name) {
        assertThrows(IllegalArgumentException.class, () -> new AcademicSpace(UUID.randomUUID(), name, "2026-2"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("RF-04 / ER: periodo obligatorio")
    void requiresPeriod(String period) {
        assertThrows(IllegalArgumentException.class, () -> new AcademicSpace(UUID.randomUUID(), "Psicologia", period));
    }

    @ParameterizedTest
    @ValueSource(ints = {151, 200})
    @DisplayName("RF-04 / ER: respeta longitud maxima del nombre")
    void limitsNameLength(int length) {
        assertThrows(IllegalArgumentException.class, () -> new AcademicSpace(UUID.randomUUID(), "x".repeat(length), "2026-2"));
    }

    @ParameterizedTest
    @ValueSource(ints = {31, 100})
    @DisplayName("RF-04 / ER: respeta longitud maxima del periodo")
    void limitsPeriodLength(int length) {
        assertThrows(IllegalArgumentException.class, () -> new AcademicSpace(UUID.randomUUID(), "Psicologia", "x".repeat(length)));
    }
}
