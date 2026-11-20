package com.psicoapp.identity.api;

import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.application.ServicioUsuarios;
import com.psicoapp.identity.application.VerificadorAccesoEstudiante;
import com.psicoapp.identity.domain.Rol;
import com.psicoapp.identity.domain.Usuario;
import com.psicoapp.identity.infrastructure.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ControladorAutenticacion.class, ControladorIdentidad.class}, properties = {
        "psicoapp.identidad.correo-administrador=", "psicoapp.identidad.clave-administrador="
})
@Import({ConfiguracionIdentidad.class, ProveedorAutenticacionIdentidad.class, AutenticacionJwt.class})
@DisplayName("RF-02 / CP-ID-009: autenticacion JWT")
class PruebaAutenticacionJwt {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder claves;
    @Autowired ObjectMapper json;
    @MockitoBean RepositorioUsuarios usuarios;
    @MockitoBean ServicioUsuarios identidades;
    @MockitoBean VerificadorAccesoEstudiante convocatorias;
    Usuario docente;

    @BeforeEach
    void preparar() {
        docente = new Usuario(UUID.randomUUID(), "docente@example.test", "Docente", Rol.DOCENTE,
                claves.encode("clave-ficticia"), true, Instant.now());
        when(usuarios.buscarPorCorreo(docente.correo())).thenReturn(Optional.of(docente));
        when(usuarios.buscarPorId(docente.id())).thenReturn(Optional.of(docente));
    }

    @Test
    void ingresoSinCsrfEmiteBearerQueAutorizaSinCrearSesion() throws Exception {
        String token = ingresar(docente.correo(), "clave-ficticia");
        assertEquals(3, token.split("\\.").length);
        var respuesta = mvc.perform(get("/api/identidad/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        assertNull(respuesta.getRequest().getSession(false));
        verify(identidades).consultarIdentidad(docente.id());
        mvc.perform(get("/api/identidad/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazaClaveIncorrectaYCamposInvalidos() throws Exception {
        mvc.perform(post("/api/identidad/ingreso").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"docente@example.test\",\"clave\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/identidad/ingreso").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"invalido\",\"clave\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaFirmaAlteradaTokenMalformadoYCuentaDesactivada() throws Exception {
        String token = ingresar(docente.correo(), "clave-ficticia");
        String[] partes = token.split("\\.");
        String alterado = partes[0] + "." + partes[1] + "." + (partes[2].startsWith("A") ? "B" : "A") + partes[2].substring(1);
        for (String invalido : new String[] {alterado, "invalido"})
            mvc.perform(get("/api/identidad/yo").header("Authorization", "Bearer " + invalido))
                    .andExpect(status().isUnauthorized());
        when(usuarios.buscarPorId(docente.id())).thenReturn(Optional.of(new Usuario(docente.id(), docente.correo(),
                docente.nombre(), docente.rol(), docente.hashClave(), false, docente.fechaCreacion())));
        mvc.perform(get("/api/identidad/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(identidades);
    }

    @Test
    void cambioDeRolInvalidaTokenAnterior() throws Exception {
        String token = ingresar(docente.correo(), "clave-ficticia");
        when(usuarios.buscarPorId(docente.id())).thenReturn(Optional.of(new Usuario(docente.id(), docente.correo(),
                docente.nombre(), Rol.ADMINISTRADOR, docente.hashClave(), true, docente.fechaCreacion())));
        mvc.perform(get("/api/identidad/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void retiroDeConvocatoriaInvalidaTokenEstudiantil() throws Exception {
        var estudiante = new Usuario(UUID.randomUUID(), "alumna@example.test", "Alumna", Rol.ESTUDIANTE,
                null, true, Instant.now());
        when(usuarios.buscarPorCorreo(estudiante.correo())).thenReturn(Optional.of(estudiante));
        when(usuarios.buscarPorId(estudiante.id())).thenReturn(Optional.of(estudiante));
        String codigo = "0123456789abcdef0123456789abcdef";
        when(convocatorias.verificar(estudiante.id(), codigo)).thenReturn(true);
        when(convocatorias.verificarResumen(eq(estudiante.id()), anyString())).thenReturn(true);
        String token = ingresar(estudiante.correo(), codigo);
        mvc.perform(get("/api/identidad/yo").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        when(convocatorias.verificarResumen(eq(estudiante.id()), anyString())).thenReturn(false);
        mvc.perform(get("/api/identidad/yo").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    private String ingresar(String correo, String clave) throws Exception {
        var respuesta = mvc.perform(post("/api/identidad/ingreso").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new ControladorAutenticacion.SolicitudIngreso(correo, clave))))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tipo").value("Bearer")).andExpect(jsonPath("$.venceEn").exists())
                .andExpect(jsonPath("$.clave").doesNotExist()).andReturn();
        return json.readTree(respuesta.getResponse().getContentAsString()).get("token").asText();
    }
}
