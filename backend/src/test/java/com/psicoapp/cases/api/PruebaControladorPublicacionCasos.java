package com.psicoapp.cases.api;

import com.psicoapp.cases.DatosPruebaCasos;
import com.psicoapp.cases.application.*;
import com.psicoapp.cases.domain.ConstructorCasoAcademico;
import com.psicoapp.identity.application.ServicioUsuarios;
import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.infrastructure.ConfiguracionIdentidad;
import com.psicoapp.identity.infrastructure.ProveedorAutenticacionIdentidad;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ControladorPublicacionCasos.class, ControladorCasos.class}, properties = {
        "psicoapp.identidad.correo-administrador=", "psicoapp.identidad.clave-administrador="
})
@Import({ConfiguracionIdentidad.class, ProveedorAutenticacionIdentidad.class,
        ServicioPublicacionCaso.class, ServicioCreacionCaso.class})
class PruebaControladorPublicacionCasos {
    @Autowired MockMvc mvc;
    @MockitoBean RepositorioUsuarios usuarios;
    @MockitoBean ServicioUsuarios identidades;
    @MockitoBean RepositorioCasos casos;

    @Test
    @DisplayName("RF-02/09: publicacion requiere autenticacion, docente y CSRF")
    void exigeAutenticacionRolYCsrf() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(post("/api/casos/{id}/publicar", id).with(csrf())).andExpect(status().isUnauthorized());
        for (String rol : new String[]{"ESTUDIANTE", "ADMINISTRADOR"}) {
            mvc.perform(post("/api/casos/{id}/publicar", id).with(csrf())
                            .with(user(UUID.randomUUID().toString()).roles(rol))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/casos/{id}/publicar", id)
                        .with(user(UUID.randomUUID().toString()).roles("DOCENTE"))).andExpect(status().isForbidden());
        verifyNoInteractions(casos, identidades);
    }

    @Test
    @DisplayName("RF-09 / CU-09: publica con actor autenticado y permite consultar el estado publicado")
    void publicaYConsulta() throws Exception {
        UUID docente = UUID.randomUUID();
        var caso = DatosPruebaCasos.completo(docente);
        when(casos.bloquearPorId(caso.id())).thenReturn(Optional.of(caso));
        when(casos.publicar(caso.id(), docente)).thenReturn(true);
        mvc.perform(post("/api/casos/{id}/publicar", caso.id()).with(csrf())
                        .with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(caso.id().toString()))
                .andExpect(jsonPath("$.estado").value("PUBLICADO"));
        var publicado = new com.psicoapp.cases.domain.Caso(caso.id(), docente, caso.nombre(),
                caso.desarrolloSituacional(), caso.contexto(), caso.tiempoEstimadoMin(), caso.escenarios(), caso.rubrica(),
                com.psicoapp.cases.domain.EstadoCaso.PUBLICADO);
        when(casos.buscarPorId(caso.id())).thenReturn(Optional.of(publicado));
        mvc.perform(get("/api/casos/{id}", caso.id()).with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PUBLICADO"));
    }

    @Test
    @DisplayName("RN-03/05: errores acumulados responden 422 con codigo, ruta y mensaje")
    void informaErroresSinPublicar() throws Exception {
        UUID docente = UUID.randomUUID();
        var caso = new ConstructorCasoAcademico(UUID.randomUUID(), docente).construir();
        when(casos.bloquearPorId(caso.id())).thenReturn(Optional.of(caso));
        mvc.perform(post("/api/casos/{id}/publicar", caso.id()).with(csrf())
                        .with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.errores.length()").value(6))
                .andExpect(jsonPath("$.errores[0].codigo").value("TEXTO_REQUERIDO"))
                .andExpect(jsonPath("$.errores[0].ruta").value("nombre"))
                .andExpect(jsonPath("$.errores[0].mensaje").isNotEmpty());
        verify(casos, never()).publicar(any(), any());
    }

    @Test
    @DisplayName("RF-02/09: permiso revocado o propietario ajeno no publican")
    void protegePermisoYPropiedad() throws Exception {
        UUID docente = UUID.randomUUID();
        var ajeno = DatosPruebaCasos.completo(UUID.randomUUID());
        when(casos.bloquearPorId(ajeno.id())).thenReturn(Optional.of(ajeno));
        mvc.perform(post("/api/casos/{id}/publicar", ajeno.id()).with(csrf())
                        .with(user(docente.toString()).roles("DOCENTE"))).andExpect(status().isForbidden());
        doThrow(new AccesoDenegado("Sin permiso")).when(identidades).exigirCreacionCasos(docente);
        mvc.perform(post("/api/casos/{id}/publicar", UUID.randomUUID()).with(csrf())
                        .with(user(docente.toString()).roles("DOCENTE"))).andExpect(status().isForbidden());
        verify(casos, never()).publicar(any(), any());
    }

    @Test
    @DisplayName("RF-09: caso inexistente y actualizacion concurrente tienen respuestas 404 y 409")
    void informaAusenciaYConflicto() throws Exception {
        UUID docente = UUID.randomUUID();
        mvc.perform(post("/api/casos/{id}/publicar", UUID.randomUUID()).with(csrf())
                        .with(user(docente.toString()).roles("DOCENTE"))).andExpect(status().isNotFound());
        var caso = DatosPruebaCasos.completo(docente);
        when(casos.bloquearPorId(caso.id())).thenReturn(Optional.of(caso));
        mvc.perform(post("/api/casos/{id}/publicar", caso.id()).with(csrf())
                        .with(user(docente.toString()).roles("DOCENTE"))).andExpect(status().isConflict());
    }
}
