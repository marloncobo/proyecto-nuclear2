package com.psicoapp.integracion;

import com.psicoapp.identity.application.ServicioUsuarios;
import com.psicoapp.sessions.application.*;
import com.jayway.jsonpath.JsonPath;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static com.psicoapp.AutenticacionPruebas.portador;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Mismo recorrido contra H2 y, de forma optativa, contra un esquema MySQL aislado. */
abstract class FlujoBackend {
    @Autowired MockMvc mvc;
    @Autowired ServicioUsuarios identidad;
    @Autowired org.springframework.transaction.PlatformTransactionManager gestor;
    @Autowired RepositorioSesiones sesiones;
    @Autowired RepositorioConvocatorias convocatorias;
    @MockitoBean Clock reloj;

    @Test
    void recorreCasosConvocatoriasYEstadosConAutenticacionReal() throws Exception {
        when(reloj.instant()).thenReturn(Instant.parse("2026-11-03T14:00:00Z"));
        String clave = "clave-ficticia";
        String admin = "admin@example.test", docente = "docente@example.test", estudiante = "alumno@example.test";
        identidad.inicializarAdministrador(admin, clave);
        String idDocente = id(mvc.perform(post("/api/identidad/docentes").with(portador(mvc, admin, clave))
                .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\""+docente+"\",\"nombre\":\"Docente\",\"clave\":\""+clave+"\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        mvc.perform(put("/api/identidad/usuarios/{id}/creacion-casos", idDocente).with(portador(mvc, admin, clave))
                .contentType(MediaType.APPLICATION_JSON).content("{\"habilitado\":true}")).andExpect(status().isOk());
        String idEstudiante = id(mvc.perform(post("/api/identidad/estudiantes").with(portador(mvc, docente, clave))
                .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\""+estudiante+"\",\"nombre\":\"Alumno\",\"clave\":\""+clave+"\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String idEspacio = id(mvc.perform(post("/api/academico/espacios").with(portador(mvc, docente, clave))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Psicologia","periodo":"2026-2","estudiantes":["%s"]}
                        """.formatted(idEstudiante)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estudiantes[0]").value(idEstudiante))
                .andReturn().getResponse().getContentAsString());
        // Estos contratos se ejecutan tambien en la variante MySQL optativa.
        mvc.perform(get("/api/identidad/usuarios").param("rol", "DOCENTE").param("activo", "true").with(portador(mvc, admin, clave)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(idDocente));
        mvc.perform(put("/api/identidad/docentes/{id}", idDocente).with(portador(mvc, admin, clave))
                .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\""+docente+"\",\"nombre\":\"Docente actualizado\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/identidad/usuarios/{id}/estado", idDocente).with(portador(mvc, admin, clave))
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}")).andExpect(status().isOk());
        rechazaIngreso(docente, clave);
        mvc.perform(put("/api/identidad/usuarios/{id}/estado", idDocente).with(portador(mvc, admin, clave))
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}")).andExpect(status().isOk());
        mvc.perform(put("/api/academico/espacios/{id}", idEspacio).with(portador(mvc, docente, clave))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Espacio actualizado\",\"periodo\":\"2026-2\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estudiantes[0]").value(idEstudiante));
        String pregunta=UUID.randomUUID().toString();
        String contenido = """
                {"nombre":"Caso","desarrolloSituacional":"Conflicto","contexto":"Educativo","tiempoEstimadoMin":40,
                "escenarios":[{"id":"%s","titulo":"Escenario","contextoSituacional":"Contexto","orden":1,
                "preguntas":[{"id":"%s","enunciado":"Como intervenir","orden":1,"opciones":[
                {"id":"%s","texto":"Escuchar","esCorrecta":true,"justificacion":"Reconoce perspectivas"},
                {"id":"%s","texto":"Imponer","esCorrecta":false,"justificacion":"Ignora intereses"}]}]}],
                "rubrica":{"id":"%s","nombre":"Rubrica","escalaMaxima":5,"criterios":[
                {"id":"%s","nombre":"Analisis","ponderacion":1,"preguntas":["%s"]}]}}
                """.formatted(UUID.randomUUID(),pregunta,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),pregunta);
        // TEXT utf8mb4: el limite Java de 16000 unidades UTF-16 queda por debajo de 65535 bytes.
        String contextoLimite = "\u0800".repeat(16000);
        assertTrue(contextoLimite.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 65535);
        contenido = contenido.replace("\"contexto\":\"Educativo\"", "\"contexto\":\""+contextoLimite+"\"");
        String caso=id(mvc.perform(post("/api/casos").with(portador(mvc, docente,clave))
                .contentType(MediaType.APPLICATION_JSON).content(contenido)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        mvc.perform(get("/api/casos/{id}",caso).with(portador(mvc, docente,clave)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contexto").value(contextoLimite));
        mvc.perform(put("/api/casos/{id}",caso).with(portador(mvc, docente,clave))
                .contentType(MediaType.APPLICATION_JSON).content(contenido.replace("\"Caso\"","\"Caso editado\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Caso editado"));
        mvc.perform(post("/api/casos/{id}/publicar",caso).with(portador(mvc, docente,clave)))
                .andExpect(status().isOk());
        mvc.perform(put("/api/casos/{id}",caso).with(portador(mvc, docente,clave))
                .contentType(MediaType.APPLICATION_JSON).content(contenido)).andExpect(status().isConflict());
        String programacion = """
                {"idCaso":"%s","idEspacio":"%s","inicioPrevisto":"2026-11-03T09:00:00-05:00",
                "finPrevisto":"2026-11-03T10:00:00-05:00","duracionMaximaMin":30,"modoNavegacion":"FLEXIBLE",
                "estudiantes":["%s"]}
                """.formatted(caso,idEspacio,idEstudiante);
        String creada=mvc.perform(post("/api/sesiones").with(portador(mvc, docente,clave))
                .contentType(MediaType.APPLICATION_JSON).content(programacion)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.convocatorias.length()").value(1)).andReturn().getResponse().getContentAsString();
        String sesion=id(creada);
        mvc.perform(put("/api/academico/espacios/{id}", idEspacio).with(portador(mvc, docente, clave))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Cambio rechazado\",\"periodo\":\"2027-1\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/academico/espacios/{id}", idEspacio).with(portador(mvc, docente, clave))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Espacio actualizado","periodo":"2026-2","estudiantes":[]}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estudiantes").isEmpty());
        mvc.perform(get("/api/sesiones/{id}", sesion).with(portador(mvc, docente, clave)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.convocatorias[0].idEstudiante").value(idEstudiante));
        String codigo=JsonPath.read(creada,"$.convocatorias[0].codigoAcceso");
        String segunda=mvc.perform(post("/api/sesiones").with(portador(mvc, docente,clave))
                .contentType(MediaType.APPLICATION_JSON).content(programacion)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertNotEquals(codigo, JsonPath.read(segunda,"$.convocatorias[0].codigoAcceso"));
        assertEquals(32,codigo.length());
        assertNotEquals(codigo,convocatorias.listarPorSesion(UUID.fromString(sesion)).getFirst().resumenCodigo());
        mvc.perform(get("/api/identidad/yo").with(portador(mvc, estudiante,clave))).andExpect(status().isOk());
        mvc.perform(post("/api/sesiones/{id}/iniciar",sesion).with(portador(mvc, docente,clave)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.aperturaReal").value("2026-11-03T14:00:00Z"));
        mvc.perform(get("/api/identidad/yo").with(portador(mvc, estudiante,clave))).andExpect(status().isOk());
        mvc.perform(get("/api/casos/{id}",caso).with(portador(mvc, estudiante,clave))).andExpect(status().isForbidden());
        rechazaIngreso(estudiante, "incorrecto");
        rechazaIngreso(docente, "incorrecta");
        mvc.perform(get("/api/sesiones/{id}",sesion).with(portador(mvc, docente,clave)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.convocatorias[0].codigoAcceso").isEmpty())
                .andExpect(jsonPath("$.aperturaReal").value("2026-11-03T14:00:00Z"));
        mvc.perform(post("/api/casos/{id}/archivar",caso).with(portador(mvc, docente,clave)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ARCHIVADO"));
        mvc.perform(post("/api/sesiones").with(portador(mvc, docente,clave))
                .contentType(MediaType.APPLICATION_JSON).content(programacion)).andExpect(status().isConflict());
        // Simula recuperacion tras reinicio: reconstruye repositorios/servicio contra el estado persistido.
        when(reloj.instant()).thenReturn(Instant.parse("2026-11-05T15:00:00Z"));
        var jdbc = org.springframework.jdbc.core.simple.JdbcClient.create(
                ((org.springframework.jdbc.datasource.DataSourceTransactionManager)gestor).getDataSource());
        var recuperador = new ServicioGestionSesiones(new com.psicoapp.sessions.infrastructure.RepositorioSesionesJdbc(jdbc),
                null,null,null,reloj,new com.psicoapp.sessions.infrastructure.RepositorioConvocatoriasJdbc(jdbc),
                mock(com.psicoapp.shared.events.NotificacionSujeto.class), (id, fecha) -> { }, evento -> { },
                "http://localhost:3000/ingreso");
        var tx = new org.springframework.transaction.support.TransactionTemplate(gestor);
        int cerradas = tx.execute(estado -> recuperador.cerrarVencidas(reloj.instant()));
        int repetidas = tx.execute(estado -> recuperador.cerrarVencidas(reloj.instant()));
        assertEquals(2,cerradas);
        assertEquals(0,repetidas);
        assertEquals("FINALIZADO",sesiones.buscarPorId(UUID.fromString(sesion)).orElseThrow().estado().codigo().name());
        mvc.perform(get("/api/sesiones/{id}", sesion).with(portador(mvc, docente, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aperturaReal").value("2026-11-03T14:00:00Z"))
                .andExpect(jsonPath("$.cierreReal").value("2026-11-05T15:00:00Z"));
        mvc.perform(get("/api/identidad/yo").with(portador(mvc, estudiante,clave))).andExpect(status().isOk());
        String clavePersonal = "clave-personal";
        String claveNueva = "clave-actualizada";
        mvc.perform(put("/api/identidad/yo/clave").with(portador(mvc, estudiante, clave))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"clave-ficticia\",\"nueva\":\"clave-actualizada\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/identidad/clave-inicial").with(portador(mvc, estudiante, clave))
                .contentType(MediaType.APPLICATION_JSON).content("{\"clave\":\"clave-personal\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/identidad/yo/clave").with(portador(mvc, estudiante, clavePersonal))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"clave-equivocada\",\"nueva\":\"clave-actualizada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La contraseña actual es incorrecta"));
        mvc.perform(put("/api/identidad/yo/clave").with(portador(mvc, estudiante, clavePersonal))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"clave-personal\",\"nueva\":\"clave-actualizada\"}"))
                .andExpect(status().isNoContent());
        rechazaIngreso(estudiante, clavePersonal);
        mvc.perform(get("/api/identidad/yo").with(portador(mvc, estudiante, claveNueva)))
                .andExpect(status().isOk());
    }

    private String id(String cuerpo) { return JsonPath.read(cuerpo,"$.id"); }

    private void rechazaIngreso(String correo, String clave) throws Exception {
        mvc.perform(post("/api/identidad/ingreso").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"clave\":\"" + clave + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
