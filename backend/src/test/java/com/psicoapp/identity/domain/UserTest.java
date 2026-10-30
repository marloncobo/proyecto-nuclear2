package com.psicoapp.identity.domain;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "correo", "a@@b.com", "a b@c.com"})
    @DisplayName("RF-01 / RF-03: correo de usuario valido y obligatorio")
    void rejectsInvalidEmail(String email) {
        assertThrows(IllegalArgumentException.class, () -> User.normalizeEmail(email));
    }

    @Test
    @DisplayName("RF-03 / ER: valida campos y limites del usuario")
    void validatesNameEmailLengthAndStaffPassword() {
        assertThrows(IllegalArgumentException.class, () -> user("", "e@uni.edu", null, Role.ESTUDIANTE));
        assertThrows(IllegalArgumentException.class, () -> user("x".repeat(121), "e@uni.edu", null, Role.ESTUDIANTE));
        assertThrows(IllegalArgumentException.class, () -> user("Usuario", "x".repeat(180) + "@uni.edu", null, Role.ESTUDIANTE));
        assertThrows(IllegalArgumentException.class, () -> user("Usuario", "e@uni.edu", null, Role.DOCENTE));
        assertDoesNotThrow(() -> user("Usuario", "e@uni.edu", null, Role.ESTUDIANTE));
    }

    private User user(String name, String email, String password, Role role) {
        return new User(UUID.randomUUID(), email, name, role, password, true, Instant.now());
    }
}
