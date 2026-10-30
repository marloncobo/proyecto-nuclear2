package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.VerificadorAccesoEstudiante;
import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.domain.Rol;
import com.psicoapp.identity.domain.Usuario;
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
class PruebaProveedorAutenticacionIdentidad {
    @Mock RepositorioUsuarios usuarios;
    @Mock ObjectProvider<VerificadorAccesoEstudiante> proveedorVerificador;
    @Mock VerificadorAccesoEstudiante verificador;
    private final BCryptPasswordEncoder claves = new BCryptPasswordEncoder(4);
    private ProveedorAutenticacionIdentidad proveedor;

    @BeforeEach
    void preparar() {
        proveedor = new ProveedorAutenticacionIdentidad(usuarios, claves, proveedorVerificador);
    }

    @Test
    @DisplayName("RF-02: autentica personal con resumen y construye identidad sin credenciales")
    void autenticaPersonalSinExponerCredenciales() {
        Usuario docente = usuario(Rol.DOCENTE, true);
        when(usuarios.buscarPorCorreo(docente.correo())).thenReturn(Optional.of(docente));
        var resultado = proveedor.authenticate(credenciales(docente.correo().toUpperCase(), "clave-ficticia"));
        assertTrue(resultado.isAuthenticated());
        assertEquals(docente.id().toString(), resultado.getName());
        assertNull(resultado.getCredentials());
        assertEquals("ROLE_DOCENTE", resultado.getAuthorities().iterator().next().getAuthority());
        verifyNoInteractions(proveedorVerificador);
    }

    @Test
    @DisplayName("RF-02: rechaza contrasena incorrecta")
    void rechazaClaveIncorrectaDelPersonal() {
        Usuario docente = usuario(Rol.DOCENTE, true);
        when(usuarios.buscarPorCorreo(docente.correo())).thenReturn(Optional.of(docente));
        assertThrows(BadCredentialsException.class, () -> proveedor.authenticate(credenciales(docente.correo(), "incorrecta")));
    }

    @Test
    @DisplayName("RF-02: no autentica usuario inexistente, inactivo ni correo invalido")
    void rechazaIdentidadInexistenteInactivaOInvalida() {
        assertThrows(BadCredentialsException.class, () -> proveedor.authenticate(credenciales("sin-correo", "clave")));
        assertThrows(BadCredentialsException.class, () -> proveedor.authenticate(credenciales("nadie@universidad.edu.co", "clave")));
        Usuario inactivo = usuario(Rol.DOCENTE, false);
        when(usuarios.buscarPorCorreo(inactivo.correo())).thenReturn(Optional.of(inactivo));
        assertThrows(BadCredentialsException.class, () -> proveedor.authenticate(credenciales(inactivo.correo(), "clave-ficticia")));
    }

    @Test
    @DisplayName("RF-01 / RN-04: estudiante requiere codigo aprobado por el contrato de convocatorias")
    void autenticaEstudianteSoloConCodigoVerificado() {
        Usuario estudiante = usuario(Rol.ESTUDIANTE, true);
        when(usuarios.buscarPorCorreo(estudiante.correo())).thenReturn(Optional.of(estudiante));
        when(proveedorVerificador.getIfAvailable()).thenReturn(verificador);
        when(verificador.verificar(estudiante.id(), "codigo-ficticio")).thenReturn(true);
        assertTrue(proveedor.authenticate(credenciales(estudiante.correo(), "codigo-ficticio")).isAuthenticated());
        verify(verificador).verificar(estudiante.id(), "codigo-ficticio");
    }

    @Test
    @DisplayName("RF-01 / RN-04: codigo invalido o integracion ausente deniega acceso")
    void rechazaEstudianteSinConvocatoriaVerificada() {
        Usuario estudiante = usuario(Rol.ESTUDIANTE, true);
        when(usuarios.buscarPorCorreo(estudiante.correo())).thenReturn(Optional.of(estudiante));
        assertThrows(BadCredentialsException.class, () -> proveedor.authenticate(credenciales(estudiante.correo(), "codigo")));
        when(proveedorVerificador.getIfAvailable()).thenReturn(verificador);
        assertThrows(BadCredentialsException.class, () -> proveedor.authenticate(credenciales(estudiante.correo(), "codigo")));
    }

    private UsernamePasswordAuthenticationToken credenciales(String correo, String secreto) {
        return UsernamePasswordAuthenticationToken.unauthenticated(correo, secreto);
    }

    private Usuario usuario(Rol rol, boolean activo) {
        return new Usuario(UUID.randomUUID(), "usuario@universidad.edu.co", "Usuario ficticio", rol,
                rol == Rol.ESTUDIANTE ? null : claves.encode("clave-ficticia"), activo, Instant.now());
    }
}
