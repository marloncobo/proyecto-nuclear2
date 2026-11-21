package com.psicoapp.reports.api;

import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.application.ServicioUsuarios;
import com.psicoapp.identity.infrastructure.ConfiguracionIdentidad;
import com.psicoapp.identity.infrastructure.ProveedorAutenticacionIdentidad;
import com.psicoapp.reports.infrastructure.ReporteSesionJdbc;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ControladorReporteSesion.class, properties = {
        "psicoapp.identidad.correo-administrador=", "psicoapp.identidad.clave-administrador="
})
@Import({ConfiguracionIdentidad.class, ProveedorAutenticacionIdentidad.class})
@DisplayName("RF-15 / CU-35: resumen HTTP de una sesion")
class PruebaControladorReporteSesion {
    @Autowired MockMvc mvc;
    @MockitoBean RepositorioUsuarios usuarios;
    @MockitoBean ServicioUsuarios servicioUsuarios;
    @MockitoBean ReporteSesionJdbc reportes;

    @Test
    void identificaAlEstudianteYExponeLaAusenciaConSuDetalle() throws Exception {
        UUID docente = UUID.randomUUID();
        UUID sesion = UUID.randomUUID();
        UUID estudiante = UUID.randomUUID();
        UUID pregunta = UUID.randomUUID();
        var ausencia = new ReporteSesionJdbc.Ausencia(Instant.EPOCH, List.of(
                new ReporteSesionJdbc.DetalleAusencia(pregunta, BigDecimal.ZERO, "Ausencia registrada")));
        when(reportes.consultar(docente, sesion)).thenReturn(List.of(new ReporteSesionJdbc.Fila(
                estudiante, "Ana Aprendiz", "ana@ejemplo.edu", null, "AUSENTE", BigDecimal.ZERO, ausencia)));

        mvc.perform(get("/api/reportes/sesiones/{id}", sesion)
                        .with(user(docente.toString()).roles("DOCENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreEstudiante").value("Ana Aprendiz"))
                .andExpect(jsonPath("$[0].correoEstudiante").value("ana@ejemplo.edu"))
                .andExpect(jsonPath("$[0].estado").value("AUSENTE"))
                .andExpect(jsonPath("$[0].calificacion").value(0))
                .andExpect(jsonPath("$[0].ausencia.detalles[0].idPregunta").value(pregunta.toString()))
                .andExpect(jsonPath("$[0].ausencia.detalles[0].puntos").value(0));
        verify(reportes).consultar(docente, sesion);
    }

    @Test
    void exigeAutenticacionYRolDocente() throws Exception {
        UUID sesion = UUID.randomUUID();
        mvc.perform(get("/api/reportes/sesiones/{id}", sesion)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reportes/sesiones/{id}", sesion)
                        .with(user(UUID.randomUUID().toString()).roles("ESTUDIANTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reportes);
    }
}
