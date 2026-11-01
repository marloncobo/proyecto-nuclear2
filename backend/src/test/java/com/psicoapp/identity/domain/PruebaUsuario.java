package com.psicoapp.identity.domain;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PruebaUsuario {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "correo", "a@@b.com", "a b@c.com"})
    @DisplayName("RF-01 / RF-03: correo de usuario valido y obligatorio")
    void rechazaCorreoInvalido(String correo) {
        assertThrows(IllegalArgumentException.class, () -> Usuario.normalizarCorreo(correo));
    }

    @Test
    @DisplayName("RF-03 / ER: valida campos y limites del usuario")
    void validaNombreCorreoYClaveDelPersonal() {
        assertThrows(IllegalArgumentException.class, () -> usuario("", "e@uni.edu", null, Rol.ESTUDIANTE));
        assertThrows(IllegalArgumentException.class, () -> usuario("x".repeat(121), "e@uni.edu", null, Rol.ESTUDIANTE));
        assertThrows(IllegalArgumentException.class, () -> usuario("Usuario", "x".repeat(180) + "@uni.edu", null, Rol.ESTUDIANTE));
        assertThrows(IllegalArgumentException.class, () -> usuario("Usuario", "e@uni.edu", null, Rol.DOCENTE));
        assertDoesNotThrow(() -> usuario("Usuario", "e@uni.edu", null, Rol.ESTUDIANTE));
    }

    private Usuario usuario(String nombre, String correo, String clave, Rol rol) {
        return new Usuario(UUID.randomUUID(), correo, nombre, rol, clave, true, Instant.now());
    }
}
