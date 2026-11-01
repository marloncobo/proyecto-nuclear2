package com.psicoapp.cases.api;

import com.psicoapp.cases.application.RepositorioCasos;
import com.psicoapp.cases.application.ServicioCreacionCaso;
import com.psicoapp.cases.domain.ConstructorCasoAcademico;
import com.psicoapp.identity.application.DirectorioIdentidad;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ControladorCasos.class, properties = {
        "psicoapp.identidad.correo-administrador=", "psicoapp.identidad.clave-administrador="
})
@Import({ConfiguracionIdentidad.class, ProveedorAutenticacionIdentidad.class, ServicioCreacionCaso.class})
class PruebaControladorCasos {
    @Autowired MockMvc mvc;
    @MockitoBean RepositorioUsuarios usuarios;
    @MockitoBean ServicioUsuarios identidades;
    @MockitoBean RepositorioCasos casos;

    @Test
    @DisplayName("RF-02: anonimos, estudiantes y administradores no consultan borradores")
    void restringeRol() throws Exception {
        String ruta = "/api/casos/" + UUID.randomUUID();
        mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        for (String rol : new String[]{"ESTUDIANTE", "ADMINISTRADOR"}) {
            mvc.perform(get(ruta).with(user(UUID.randomUUID().toString()).roles(rol)))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(casos, identidades);
    }

    @Test
    @DisplayName("RF-02: crear caso exige CSRF y permiso especifico CREAR_CASOS")
    void exigeCsrfYPermiso() throws Exception {
        UUID docente = UUID.randomUUID();
        mvc.perform(post("/api/casos").with(user(docente.toString()).roles("DOCENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"escenarios\":[]}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(identidades);
        doThrow(new AccesoDenegado("Sin permiso")).when(identidades).exigirCreacionCasos(docente);
        mvc.perform(post("/api/casos").with(user(docente.toString()).roles("DOCENTE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"escenarios\":[]}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(casos);
    }

    @Test
    @DisplayName("RF-05/09: crea borrador con actor autenticado e ignora estado enviado")
    void creaConIdentidadAutenticada() throws Exception {
        UUID docente = UUID.randomUUID();
        mvc.perform(post("/api/casos").with(user(docente.toString()).roles("DOCENTE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Caso","tiempoEstimadoMin":40,"escenarios":[],
                                 "idDocente":"00000000-0000-0000-0000-000000000001","estado":"PUBLICADO"}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.idDocente").value(docente.toString()));
        verify(casos).guardar(argThat(caso -> caso.idDocente().equals(docente) && caso.nombre().equals("Caso")));
    }

    @Test
    @DisplayName("RF-05: tiempo invalido y contenido anidado invalido responden 400")
    void rechazaContenidoInvalido() throws Exception {
        UUID docente = UUID.randomUUID();
        for (String contenido : new String[]{
                "{\"tiempoEstimadoMin\":0,\"escenarios\":[]}",
                "{\"escenarios\":[{\"id\":null,\"orden\":1,\"preguntas\":[]}]}",
                "{\"escenarios\":[null]}",
                "{\"escenarios\":null}"}) {
            mvc.perform(post("/api/casos").with(user(docente.toString()).roles("DOCENTE")).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(contenido))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(casos);
    }

    @Test
    @DisplayName("RF-02 / CU-04: consulta propia, ajena e inexistente")
    void consultaProtegida() throws Exception {
        UUID docente = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(casos.buscarPorId(id)).thenReturn(Optional.of(new ConstructorCasoAcademico(id, docente).construir()));
        mvc.perform(get("/api/casos/{id}", id).with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("BORRADOR"));
        mvc.perform(get("/api/casos/{id}", id).with(user(UUID.randomUUID().toString()).roles("DOCENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/casos/{id}", UUID.randomUUID()).with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isNotFound());
    }
}
