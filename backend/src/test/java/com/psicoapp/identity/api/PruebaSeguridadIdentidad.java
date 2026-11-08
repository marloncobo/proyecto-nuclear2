package com.psicoapp.identity.api;

import com.psicoapp.academic.api.ControladorEspaciosAcademicos;
import com.psicoapp.academic.application.FachadaAdministracion;
import com.psicoapp.identity.application.DirectorioIdentidad.DatosUsuario;
import com.psicoapp.identity.application.ServicioUsuarios;
import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.domain.Rol;
import com.psicoapp.identity.domain.Usuario;
import com.psicoapp.identity.infrastructure.ProveedorAutenticacionIdentidad;
import com.psicoapp.identity.infrastructure.ConfiguracionIdentidad;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ControladorIdentidad.class, ControladorEspaciosAcademicos.class}, properties = {
        "psicoapp.identidad.correo-administrador=", "psicoapp.identidad.clave-administrador="
})
@Import({ConfiguracionIdentidad.class, ProveedorAutenticacionIdentidad.class})
class PruebaSeguridadIdentidad {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder claves;
    @MockitoBean RepositorioUsuarios usuarios;
    @MockitoBean ServicioUsuarios identidades;
    @MockitoBean FachadaAdministracion academico;

    @Test
    @DisplayName("RF-02: solicitudes anonimas no consultan datos de identidad")
    void solicitudAnonimaNoEstaAutorizada() throws Exception {
        mvc.perform(get("/api/identidad/yo")).andExpect(status().isUnauthorized());
        verifyNoInteractions(identidades);
    }

    @Test
    @DisplayName("RF-02: personal con credenciales validas consulta su propia identidad")
    void autenticaCredencialesBasicasReales() throws Exception {
        var docente = new Usuario(UUID.randomUUID(), "docente@uni.edu", "Docente", Rol.DOCENTE,
                claves.encode("clave-ficticia"), true, Instant.now());
        when(usuarios.buscarPorCorreo(docente.correo())).thenReturn(Optional.of(docente));
        when(identidades.consultarIdentidad(docente.id())).thenReturn(new DatosUsuario(docente.id(), docente.correo(), "Docente", "DOCENTE", false));
        mvc.perform(get("/api/identidad/yo").with(httpBasic(docente.correo(), "clave-ficticia")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(docente.id().toString()))
                .andExpect(jsonPath("$.hashClave").doesNotExist());
        mvc.perform(get("/api/identidad/yo").with(httpBasic(docente.correo(), "incorrecta")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-10: docente no crea cuentas docentes aunque envie un identificador valido")
    void docenteNoRegistraOtroDocente() throws Exception {
        mvc.perform(post("/api/identidad/docentes").with(user(UUID.randomUUID().toString()).roles("DOCENTE"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"otro@uni.edu\",\"nombre\":\"Otro\",\"clave\":\"clave-ficticia\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(identidades);
    }

    @Test
    @DisplayName("RN-10: administrador crea docente y valida campos HTTP")
    void administradorRegistraDocenteYRechazaDatosInvalidos() throws Exception {
        UUID idAdministrador = UUID.randomUUID();
        when(identidades.registrarDocente(eq(idAdministrador), anyString(), anyString(), anyString()))
                .thenReturn(new DatosUsuario(UUID.randomUUID(), "otro@uni.edu", "Otro", "DOCENTE", false));
        mvc.perform(post("/api/identidad/docentes").with(user(idAdministrador.toString()).roles("ADMINISTRADOR"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"otro@uni.edu\",\"nombre\":\"Otro\",\"clave\":\"clave-ficticia\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/identidad/docentes").with(user(idAdministrador.toString()).roles("ADMINISTRADOR"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"invalido\",\"nombre\":\"\",\"clave\":\"corta\"}"))
                .andExpect(status().isBadRequest());
        verify(identidades, times(1)).registrarDocente(eq(idAdministrador), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("RF-02: mutaciones autenticadas requieren token CSRF")
    void rechazaModificacionSinCsrf() throws Exception {
        mvc.perform(post("/api/academico/espacios").with(user(UUID.randomUUID().toString()).roles("DOCENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Psicologia\",\"periodo\":\"2026-2\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(academico);
    }

    @Test
    @DisplayName("RF-02 / RF-04: estudiante no administra espacios academicos")
    void estudianteNoAdministraEspaciosAcademicos() throws Exception {
        mvc.perform(get("/api/academico/espacios").with(user(UUID.randomUUID().toString()).roles("ESTUDIANTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(academico);
    }

    @Test
    @DisplayName("RF-04: rutas y campos del espacio academico estan en espanol")
    void docenteCreaYConsultaEspacioConCamposEnEspanol() throws Exception {
        UUID idDocente = UUID.randomUUID();
        UUID idEspacio = UUID.randomUUID();
        var espacio = new FachadaAdministracion.DatosEspacioAcademico(idEspacio, "Psicologia", "2026-2");
        when(academico.crearEspacio(idDocente, "Psicologia", "2026-2")).thenReturn(espacio);
        when(academico.consultarEspacio(idDocente, idEspacio)).thenReturn(espacio);
        mvc.perform(post("/api/academico/espacios").with(user(idDocente.toString()).roles("DOCENTE"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Psicologia\",\"periodo\":\"2026-2\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nombre").value("Psicologia"))
                .andExpect(jsonPath("$.periodo").value("2026-2"))
                .andExpect(jsonPath("$.name").doesNotExist()).andExpect(jsonPath("$.period").doesNotExist());
        mvc.perform(get("/api/academico/espacios/{id}", idEspacio)
                        .with(user(idDocente.toString()).roles("DOCENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(idEspacio.toString()));
    }

    @Test
    @DisplayName("RF-02: administrador configura permisos mediante ruta y campo en espanol")
    void administradorConfiguraPermisosConCamposEnEspanol() throws Exception {
        UUID idAdministrador = UUID.randomUUID();
        mvc.perform(put("/api/identidad/roles/DOCENTE/creacion-casos")
                        .with(user(idAdministrador.toString()).roles("ADMINISTRADOR"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"habilitado\":true}"))
                .andExpect(status().isNoContent());
        verify(identidades).configurarCreacionCasos(idAdministrador, Rol.DOCENTE, true);
    }
}
