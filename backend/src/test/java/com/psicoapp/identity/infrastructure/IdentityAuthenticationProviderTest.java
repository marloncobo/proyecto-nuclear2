package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.StudentAccessVerifier;
import com.psicoapp.identity.application.UserRepository;
import com.psicoapp.identity.domain.Role;
import com.psicoapp.identity.domain.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityAuthenticationProviderTest {
    @Mock UserRepository users;
    @Mock ObjectProvider<StudentAccessVerifier> verifierProvider;
    @Mock StudentAccessVerifier verifier;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);
    private IdentityAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new IdentityAuthenticationProvider(users, passwords, verifierProvider);
    }

    @Test
    @DisplayName("RF-02: autentica personal con hash y construye identidad sin credenciales")
    void authenticatesStaffWithoutExposingCredentials() {
        User teacher = user(Role.DOCENTE, true);
        when(users.findByEmail(teacher.email())).thenReturn(Optional.of(teacher));
        var result = provider.authenticate(credentials(teacher.email().toUpperCase(), "clave-ficticia"));
        assertTrue(result.isAuthenticated());
        assertEquals(teacher.id().toString(), result.getName());
        assertNull(result.getCredentials());
        assertEquals("ROLE_DOCENTE", result.getAuthorities().iterator().next().getAuthority());
        verifyNoInteractions(verifierProvider);
    }

    @Test
    @DisplayName("RF-02: rechaza contrasena incorrecta")
    void rejectsWrongStaffPassword() {
        User teacher = user(Role.DOCENTE, true);
        when(users.findByEmail(teacher.email())).thenReturn(Optional.of(teacher));
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(credentials(teacher.email(), "incorrecta")));
    }

    @Test
    @DisplayName("RF-02: no autentica usuario inexistente, inactivo ni correo invalido")
    void rejectsMissingInactiveAndMalformedIdentity() {
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(credentials("sin-correo", "clave")));
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(credentials("nadie@universidad.edu.co", "clave")));
        User inactive = user(Role.DOCENTE, false);
        when(users.findByEmail(inactive.email())).thenReturn(Optional.of(inactive));
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(credentials(inactive.email(), "clave-ficticia")));
    }

    @Test
    @DisplayName("RF-01 / RN-04: estudiante requiere codigo aprobado por el contrato de convocatorias")
    void authenticatesStudentOnlyWithVerifiedCode() {
        User student = user(Role.ESTUDIANTE, true);
        when(users.findByEmail(student.email())).thenReturn(Optional.of(student));
        when(verifierProvider.getIfAvailable()).thenReturn(verifier);
        when(verifier.verifies(student.id(), "codigo-ficticio")).thenReturn(true);
        assertTrue(provider.authenticate(credentials(student.email(), "codigo-ficticio")).isAuthenticated());
        verify(verifier).verifies(student.id(), "codigo-ficticio");
    }

    @Test
    @DisplayName("RF-01 / RN-04: codigo invalido o integracion ausente deniega acceso")
    void rejectsStudentWithoutVerifiedConvocation() {
        User student = user(Role.ESTUDIANTE, true);
        when(users.findByEmail(student.email())).thenReturn(Optional.of(student));
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(credentials(student.email(), "codigo")));
        when(verifierProvider.getIfAvailable()).thenReturn(verifier);
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(credentials(student.email(), "codigo")));
    }

    private UsernamePasswordAuthenticationToken credentials(String email, String secret) {
        return UsernamePasswordAuthenticationToken.unauthenticated(email, secret);
    }

    private User user(Role role, boolean active) {
        return new User(UUID.randomUUID(), "usuario@universidad.edu.co", "Usuario ficticio", role,
                role == Role.ESTUDIANTE ? null : passwords.encode("clave-ficticia"), active, Instant.now());
    }
}
