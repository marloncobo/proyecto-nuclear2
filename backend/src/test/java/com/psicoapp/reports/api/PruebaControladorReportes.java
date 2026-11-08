package com.psicoapp.reports.api;

import com.psicoapp.evaluation.application.ConsultaResultados;
import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.application.ServicioUsuarios;
import com.psicoapp.identity.infrastructure.ConfiguracionIdentidad;
import com.psicoapp.identity.infrastructure.ProveedorAutenticacionIdentidad;
import com.psicoapp.participation.application.ConsultaParticipaciones;
import com.psicoapp.participation.application.ConsultaParticipaciones.ParticipacionFinalizada;
import com.psicoapp.reports.application.FachadaConsultaParticipaciones;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ControladorReportes.class, properties = {
        "psicoapp.identidad.correo-administrador=", "psicoapp.identidad.clave-administrador="
})
@Import({ConfiguracionIdentidad.class, ProveedorAutenticacionIdentidad.class, FachadaConsultaParticipaciones.class})
@DisplayName("RF-15 / CU-30: contrato HTTP protegido de reportes")
class PruebaControladorReportes {
    @Autowired MockMvc mvc;
    @MockitoBean RepositorioUsuarios usuarios;
    @MockitoBean ServicioUsuarios servicioUsuarios;
    @MockitoBean ConsultaParticipaciones participaciones;
    @MockitoBean ConsultaResultados resultados;
    private final UUID docente = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();
    private final UUID caso = UUID.randomUUID();
    private final String ruta = "/api/reportes/casos/" + caso + "/estudiantes/" + estudiante;

    @Test
    void exigeAutenticacionYRolDocente() throws Exception {
        mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        for (String rol : List.of("ESTUDIANTE", "ADMINISTRADOR")) {
            mvc.perform(get(ruta).with(user(UUID.randomUUID().toString()).roles(rol)))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(participaciones, resultados);
    }

    @Test
    void usaIdentidadAutenticadaYRepresentaEvaluacionPendiente() throws Exception {
        UUID intento = UUID.randomUUID();
        UUID pregunta = UUID.randomUUID();
        UUID opcion = UUID.randomUUID();
        when(participaciones.consultarPorEstudiante(docente, estudiante, caso)).thenReturn(List.of(
                new ParticipacionFinalizada(intento, UUID.randomUUID(), estudiante, "FINALIZADO",
                        Instant.EPOCH, Instant.EPOCH.plusSeconds(1800), List.of(pregunta), Map.of(pregunta, opcion))));
        when(resultados.consultar(docente, intento)).thenReturn(Optional.empty());
        mvc.perform(get(ruta).with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].participacion.idIntento").value(intento.toString()))
                .andExpect(jsonPath("$[0].participacion.respuestas['" + pregunta + "']").value(opcion.toString()))
                .andExpect(jsonPath("$[0].resultado").isEmpty());
        verify(participaciones).consultarPorEstudiante(docente, estudiante, caso);
    }

    @Test
    void devuelveErroresDePropiedadExistenciaYParametros() throws Exception {
        when(participaciones.consultarPorEstudiante(docente, estudiante, caso))
                .thenThrow(new AccesoDenegado("Caso ajeno"))
                .thenThrow(new NoEncontrado("Caso no encontrado"));
        mvc.perform(get(ruta).with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get(ruta).with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/reportes/casos/invalido/estudiantes/{id}", estudiante)
                        .with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isBadRequest());
    }
}
