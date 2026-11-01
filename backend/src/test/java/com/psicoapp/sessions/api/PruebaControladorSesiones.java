package com.psicoapp.sessions.api;

import com.psicoapp.academic.application.FachadaAdministracion;
import com.psicoapp.cases.application.CatalogoCasos;
import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.application.ServicioUsuarios;
import com.psicoapp.identity.infrastructure.ConfiguracionIdentidad;
import com.psicoapp.identity.infrastructure.ProveedorAutenticacionIdentidad;
import com.psicoapp.sessions.application.*;
import com.psicoapp.sessions.DatosPruebaSesiones;
import com.psicoapp.sessions.domain.*;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static com.psicoapp.sessions.DatosPruebaSesiones.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ControladorSesiones.class, properties = {
        "psicoapp.identidad.correo-administrador=", "psicoapp.identidad.clave-administrador="
})
@Import({ConfiguracionIdentidad.class, ProveedorAutenticacionIdentidad.class, ServicioGestionSesiones.class})
class PruebaControladorSesiones {
    @Autowired MockMvc mvc;
    @MockitoBean RepositorioSesiones sesiones;
    @MockitoBean ServicioUsuarios identidades;
    @MockitoBean RepositorioUsuarios usuarios;
    @MockitoBean CatalogoCasos casos;
    @MockitoBean FachadaAdministracion academico;
    @MockitoBean Clock reloj;
    private final UUID docente = UUID.randomUUID();
    private final Sesion sesion = DatosPruebaSesiones.programada(docente);

    @BeforeEach
    void preparar() {
        when(reloj.instant()).thenReturn(INICIO);
    }

    @Test
    @DisplayName("RF-02: rutas de sesiones exigen docente y mutaciones exigen CSRF")
    void restringeAcceso() throws Exception {
        mvc.perform(get("/api/sesiones/{id}", sesion.id())).andExpect(status().isUnauthorized());
        for (String rol : new String[]{"ESTUDIANTE", "ADMINISTRADOR"}) {
            mvc.perform(get("/api/sesiones/{id}", sesion.id()).with(user(docente.toString()).roles(rol)))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/sesiones/{id}/iniciar", sesion.id()).with(csrf())
                            .with(user(docente.toString()).roles(rol))).andExpect(status().isForbidden());
        }
        for (String accion : new String[]{"iniciar", "finalizar"}) {
            mvc.perform(post("/api/sesiones/{id}/" + accion, sesion.id())
                            .with(user(docente.toString()).roles("DOCENTE"))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/sesiones").with(user(docente.toString()).roles("DOCENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(sesiones, casos, academico, identidades);
    }

    @Test
    @DisplayName("RF-18: programa SIN_INICIAR con docente autenticado, fechas UTC y campos en espanol")
    void programaDesdeIdentidadAutenticada() throws Exception {
        when(casos.exigirPublicado(docente, sesion.idCaso())).thenReturn(new CatalogoCasos.CasoProgramable(sesion.idCaso(), 40));
        mvc.perform(post("/api/sesiones").with(csrf()).with(user(docente.toString()).roles("DOCENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idCaso":"%s","idEspacio":"%s","inicioPrevisto":"2026-11-01T09:00:00-05:00",
                                 "finPrevisto":"2026-11-01T10:00:00-05:00","duracionMaximaMin":30,
                                 "modoNavegacion":"FLEXIBLE","lugar":"Aula",
                                 "idDocente":"00000000-0000-0000-0000-000000000001","estado":"FINALIZADO"}
                                """.formatted(sesion.idCaso(), sesion.idEspacio())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("SIN_INICIAR"))
                .andExpect(jsonPath("$.idDocente").value(docente.toString()))
                .andExpect(jsonPath("$.inicioPrevisto").value("2026-11-01T14:00:00Z"));
        verify(sesiones).guardar(argThat(s -> s.idDocente().equals(docente) && s.inicioPrevisto().equals(INICIO)));
    }

    @Test
    @DisplayName("RF-19: inicia y finaliza por HTTP; cierre repetido no escribe otra vez")
    void administraCicloDeVida() throws Exception {
        when(sesiones.bloquearPorId(sesion.id())).thenReturn(Optional.of(sesion));
        mvc.perform(post("/api/sesiones/{id}/iniciar", sesion.id()).with(csrf())
                        .with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_CURSO"));
        var iniciada = sesion.iniciar(INICIO);
        when(sesiones.bloquearPorId(sesion.id())).thenReturn(Optional.of(iniciada));
        mvc.perform(post("/api/sesiones/{id}/finalizar", sesion.id()).with(csrf())
                        .with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("FINALIZADO"));
        var cerrada = iniciada.finalizar(MotivoCierre.DOCENTE, FIN);
        when(sesiones.bloquearPorId(sesion.id())).thenReturn(Optional.of(cerrada));
        mvc.perform(post("/api/sesiones/{id}/finalizar", sesion.id()).with(csrf())
                        .with(user(docente.toString()).roles("DOCENTE"))).andExpect(status().isOk());
        mvc.perform(post("/api/sesiones/{id}/iniciar", sesion.id()).with(csrf())
                        .with(user(docente.toString()).roles("DOCENTE"))).andExpect(status().isConflict());
        verify(sesiones, times(2)).actualizarEstado(any());
    }

    @Test
    @DisplayName("RF-02/19: propietario ajeno, identidad inactiva e inexistente se rechazan")
    void protegePropiedadYExistencia() throws Exception {
        when(sesiones.bloquearPorId(sesion.id())).thenReturn(Optional.of(sesion));
        when(sesiones.buscarPorId(sesion.id())).thenReturn(Optional.of(sesion));
        mvc.perform(get("/api/sesiones/{id}", sesion.id()).with(user(UUID.randomUUID().toString()).roles("DOCENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/sesiones/{id}/iniciar", sesion.id()).with(csrf())
                        .with(user(UUID.randomUUID().toString()).roles("DOCENTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/sesiones/{id}", UUID.randomUUID()).with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isNotFound());
        doThrow(new AccesoDenegado("Inactivo")).when(identidades).exigirDocente(docente);
        mvc.perform(get("/api/sesiones/{id}", sesion.id()).with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isForbidden());
        verify(sesiones, never()).actualizarEstado(any());
    }

    @Test
    @DisplayName("RF-18: fechas invalidas o datos incompletos responden 400 sin guardar")
    void rechazaDatosInvalidos() throws Exception {
        mvc.perform(post("/api/sesiones").with(csrf()).with(user(docente.toString()).roles("DOCENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        when(casos.exigirPublicado(docente, sesion.idCaso())).thenReturn(new CatalogoCasos.CasoProgramable(sesion.idCaso(), 40));
        mvc.perform(post("/api/sesiones").with(csrf()).with(user(docente.toString()).roles("DOCENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idCaso":"%s","idEspacio":"%s","inicioPrevisto":"2026-11-01T16:00:00Z",
                                "finPrevisto":"2026-11-01T15:00:00Z","duracionMaximaMin":30,"modoNavegacion":"FLEXIBLE"}
                                """.formatted(sesion.idCaso(), sesion.idEspacio()))).andExpect(status().isBadRequest());
        verify(sesiones, never()).guardar(any());
    }
}
