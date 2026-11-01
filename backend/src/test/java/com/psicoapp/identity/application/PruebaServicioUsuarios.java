package com.psicoapp.identity.application;

import com.psicoapp.identity.domain.Rol;
import com.psicoapp.identity.domain.Usuario;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.Conflicto;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PruebaServicioUsuarios {
    @Mock RepositorioUsuarios usuarios;
    @Mock PermisosRol permisos;
    private final BCryptPasswordEncoder claves = new BCryptPasswordEncoder(4);
    private ServicioUsuarios servicio;
    private Usuario administrador;
    private Usuario docente;

    @BeforeEach
    void preparar() {
        servicio = new ServicioUsuarios(usuarios, claves, permisos, "universidad.edu.co, otra.edu.co");
        administrador = usuario(Rol.ADMINISTRADOR, "administrador@universidad.edu.co");
        docente = usuario(Rol.DOCENTE, "docente@universidad.edu.co");
    }

    @Test
    @DisplayName("RF-03 / RN-10: solo administrador crea docentes y almacena un resumen")
    void administradorRegistraDocenteConClaveCifrada() {
        when(usuarios.buscarPorId(administrador.id())).thenReturn(Optional.of(administrador));
        var resultado = servicio.registrarDocente(administrador.id(), " DOCENTE@universidad.edu.co ", " Docente ", "clave-ficticia");
        var guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(guardado.capture());
        assertEquals("docente@universidad.edu.co", resultado.correo());
        assertEquals("Docente", resultado.nombre());
        assertEquals("DOCENTE", resultado.rol());
        assertTrue(guardado.getValue().activo());
        assertTrue(claves.matches("clave-ficticia", guardado.getValue().hashClave()));
        assertNotEquals("clave-ficticia", guardado.getValue().hashClave());
    }

    @Test
    @DisplayName("RN-10: docente no puede administrar docentes ni permisos")
    void docenteNoAdministraUsuariosNiPermisos() {
        when(usuarios.buscarPorId(docente.id())).thenReturn(Optional.of(docente));
        assertThrows(AccesoDenegado.class, () -> servicio.registrarDocente(docente.id(), "otro@universidad.edu.co", "Otro", "clave-ficticia"));
        assertThrows(AccesoDenegado.class, () -> servicio.cambiarRol(docente.id(), administrador.id(), Rol.DOCENTE));
        assertThrows(AccesoDenegado.class, () -> servicio.configurarCreacionCasos(docente.id(), Rol.DOCENTE, true));
        verify(usuarios, never()).guardar(any());
        verifyNoInteractions(permisos);
    }

    @Test
    @DisplayName("RF-03: rechaza correo docente duplicado sin sobrescribirlo")
    void rechazaCorreoDuplicado() {
        when(usuarios.buscarPorId(administrador.id())).thenReturn(Optional.of(administrador));
        when(usuarios.buscarPorCorreo(docente.correo())).thenReturn(Optional.of(docente));
        assertThrows(Conflicto.class, () -> servicio.registrarDocente(administrador.id(), docente.correo(), "Otro", "clave-ficticia"));
        verify(usuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("RF-03: rechaza contrasena corta y BCrypt de mas de 72 bytes")
    void rechazaClavesInvalidas() {
        when(usuarios.buscarPorId(administrador.id())).thenReturn(Optional.of(administrador));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarDocente(administrador.id(), docente.correo(), "Docente", "corta"));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarDocente(administrador.id(), docente.correo(), "Docente", "\u00f1".repeat(40)));
        verify(usuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("RF-02 / RN-10: administrador asigna rol a una cuenta existente")
    void administradorCambiaRol() {
        when(usuarios.buscarPorId(administrador.id())).thenReturn(Optional.of(administrador));
        when(usuarios.buscarPorId(docente.id())).thenReturn(Optional.of(docente));
        assertEquals("ADMINISTRADOR", servicio.cambiarRol(administrador.id(), docente.id(), Rol.ADMINISTRADOR).rol());
        verify(usuarios).guardar(docente.conRol(Rol.ADMINISTRADOR));
    }

    @Test
    @DisplayName("RF-03: administrador no puede retirar su propio acceso")
    void impideRetirarRolPropioDeAdministrador() {
        when(usuarios.buscarPorId(administrador.id())).thenReturn(Optional.of(administrador));
        assertThrows(Conflicto.class, () -> servicio.cambiarRol(administrador.id(), administrador.id(), Rol.DOCENTE));
    }

    @Test
    @DisplayName("RF-02 / ER ROL_PERMISO: administrador concede y retira permiso al rol docente")
    void administradorGestionaPermisosDelRol() {
        when(usuarios.buscarPorId(administrador.id())).thenReturn(Optional.of(administrador));
        servicio.configurarCreacionCasos(administrador.id(), Rol.DOCENTE, true);
        servicio.configurarCreacionCasos(administrador.id(), Rol.DOCENTE, false);
        verify(permisos).configurarCreacionCasos(Rol.DOCENTE, true);
        verify(permisos).configurarCreacionCasos(Rol.DOCENTE, false);
        assertThrows(IllegalArgumentException.class, () -> servicio.configurarCreacionCasos(administrador.id(), Rol.ESTUDIANTE, true));
    }

    @Test
    @DisplayName("RF-02: el rol docente por si solo no autoriza crear casos")
    void exigePermisoEspecificoParaCasos() {
        when(usuarios.buscarPorId(docente.id())).thenReturn(Optional.of(docente));
        when(permisos.puedeCrearCasos(Rol.DOCENTE)).thenReturn(false, true);
        assertThrows(AccesoDenegado.class, () -> servicio.exigirCreacionCasos(docente.id()));
        assertDoesNotThrow(() -> servicio.exigirCreacionCasos(docente.id()));
    }

    @Test
    @DisplayName("RF-01 / RF-04: docente registra estudiante institucional sin contrasena")
    void registraEstudianteInstitucional() {
        when(usuarios.buscarPorId(docente.id())).thenReturn(Optional.of(docente));
        var resultado = servicio.registrarEstudiante(docente.id(), " ESTUDIANTE@UNIVERSIDAD.EDU.CO ", "Estudiante");
        assertEquals("ESTUDIANTE", resultado.rol());
        assertEquals("estudiante@universidad.edu.co", resultado.correo());
        verify(usuarios).guardar(argThat(usuario -> usuario.hashClave() == null && usuario.rol() == Rol.ESTUDIANTE));
    }

    @Test
    @DisplayName("RF-01: rechaza dominio externo y sufijos que imitan el institucional")
    void rechazaCorreoNoInstitucional() {
        when(usuarios.buscarPorId(docente.id())).thenReturn(Optional.of(docente));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarEstudiante(docente.id(), "e@externo.com", "Estudiante"));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarEstudiante(docente.id(), "e@universidad.edu.co.evil.com", "Estudiante"));
        verify(usuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("RF-04: reutiliza estudiante existente sin duplicar su identidad")
    void reutilizaEstudianteExistente() {
        Usuario estudiante = usuario(Rol.ESTUDIANTE, "estudiante@universidad.edu.co");
        when(usuarios.buscarPorId(docente.id())).thenReturn(Optional.of(docente));
        when(usuarios.buscarPorCorreo(estudiante.correo())).thenReturn(Optional.of(estudiante));
        assertEquals(estudiante.id(), servicio.registrarEstudiante(docente.id(), estudiante.correo(), "Estudiante").id());
        verify(usuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("RF-04: no transforma un correo docente en cuenta estudiante")
    void rechazaCorreoDelPersonalParaEstudiante() {
        when(usuarios.buscarPorId(docente.id())).thenReturn(Optional.of(docente));
        when(usuarios.buscarPorCorreo(docente.correo())).thenReturn(Optional.of(docente));
        assertThrows(Conflicto.class, () -> servicio.registrarEstudiante(docente.id(), docente.correo(), "Estudiante"));
    }

    @Test
    @DisplayName("RF-02: cuenta inexistente o inactiva no accede como docente")
    void usuarioInexistenteOInactivoNoAccede() {
        assertThrows(NoEncontrado.class, () -> servicio.exigirDocente(UUID.randomUUID()));
        Usuario inactivo = new Usuario(docente.id(), docente.correo(), docente.nombre(), docente.rol(), docente.hashClave(), false, docente.fechaCreacion());
        when(usuarios.buscarPorId(docente.id())).thenReturn(Optional.of(inactivo));
        assertThrows(AccesoDenegado.class, () -> servicio.exigirDocente(docente.id()));
    }

    @Test
    @DisplayName("RF-03: arranque no reemplaza credenciales de un administrador existente")
    void arranqueNoSobrescribeCuentaExistente() {
        when(usuarios.buscarPorCorreo(administrador.correo())).thenReturn(Optional.of(administrador));
        servicio.inicializarAdministrador(administrador.correo(), "otra-clave-ficticia");
        verify(usuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("RF-03: configura administrador inicial sin credenciales predeterminadas")
    void arranqueCreaAdministradorSoloSiEstaConfigurado() {
        servicio.inicializarAdministrador("", "");
        verifyNoInteractions(usuarios);
        servicio.inicializarAdministrador(administrador.correo(), "clave-ficticia");
        verify(usuarios).guardar(argThat(usuario -> usuario.rol() == Rol.ADMINISTRADOR));
    }

    private Usuario usuario(Rol rol, String correo) {
        return new Usuario(UUID.randomUUID(), correo, "Usuario ficticio", rol,
                rol == Rol.ESTUDIANTE ? null : claves.encode("clave-ficticia"), true, Instant.now());
    }
}
