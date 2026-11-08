package com.psicoapp.notifications;

import com.psicoapp.identity.application.*;
import com.psicoapp.identity.domain.Rol;
import com.psicoapp.academic.application.FachadaAdministracion;
import com.psicoapp.cases.DatosPruebaCasos;
import com.psicoapp.cases.application.*;
import com.psicoapp.sessions.application.*;
import com.psicoapp.sessions.domain.ModoNavegacion;
import com.psicoapp.participation.application.ServicioGestionParticipacion;
import com.psicoapp.evaluation.application.*;
import com.psicoapp.notifications.application.*;
import com.psicoapp.notifications.infrastructure.*;
import com.psicoapp.shared.events.*;
import com.psicoapp.shared.exceptions.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:notificaciones;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "psicoapp.identidad.dominios-institucionales=universidad.example",
    "psicoapp.identidad.correo-administrador=", "psicoapp.identidad.clave-administrador=",
    "psicoapp.notificaciones.clave-cifrado=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "psicoapp.notificaciones.url-acceso=https://universidad.example/ingreso",
    "psicoapp.notificaciones.despacho-habilitado=false",
    "psicoapp.sesiones.cierre-automatico-habilitado=false",
    "psicoapp.participacion.cierre-automatico-habilitado=false"
})
@AutoConfigureMockMvc
class PruebaFlujosNotificaciones {
    private static final Instant AHORA = Instant.parse("2026-11-08T15:00:00Z");
    @Autowired ServicioUsuarios identidades;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired FachadaAdministracion academico;
    @Autowired ServicioCreacionCaso creacion;
    @Autowired ServicioPublicacionCaso publicacion;
    @Autowired ServicioGestionSesiones sesiones;
    @Autowired ServicioGestionParticipacion participacion;
    @Autowired ServicioEvaluacion evaluaciones;
    @Autowired ServicioValoraciones valoraciones;
    @Autowired NotificacionSujeto eventos;
    @Autowired ServicioEntregaNotificaciones entrega;
    @Autowired RepositorioEntregas pendientes;
    @Autowired RepositorioAvisos avisos;
    @Autowired JdbcClient jdbc;
    @Autowired PlatformTransactionManager gestor;
    @Autowired CifradorNotificaciones cifrador;
    @Autowired MockMvc mvc;
    @MockitoBean Clock reloj;
    @MockitoBean PuertoCorreo correo;
    UUID docente, estudiante, espacio, caso, pregunta, opcion, sesion;
    String codigo, correoEstudiante;
    ServicioGestionSesiones.DatosProgramacion programacion;

    @BeforeEach
    void preparar() {
        when(reloj.instant()).thenReturn(AHORA);
        String sufijo = UUID.randomUUID().toString();
        String admin = "admin-" + sufijo + "@universidad.example";
        identidades.inicializarAdministrador(admin, "clave-ficticia");
        UUID administrador = usuarios.buscarPorCorreo(admin).orElseThrow().id();
        docente = identidades.registrarDocente(administrador, "docente-" + sufijo + "@universidad.example", "Docente", "clave-ficticia").id();
        identidades.configurarCreacionCasos(administrador, Rol.DOCENTE, true);
        correoEstudiante = "alumno-" + sufijo + "@universidad.example";
        estudiante = identidades.registrarEstudiante(docente, correoEstudiante, "Estudiante").id();
        espacio = academico.crearEspacio(docente, "Psicologia", "2026-2").id();
        var datos = DatosPruebaCasos.completo(docente);
        pregunta = datos.escenarios().getFirst().preguntas().getFirst().id();
        opcion = datos.escenarios().getFirst().preguntas().getFirst().opciones().stream().filter(o -> o.esCorrecta()).findFirst().orElseThrow().id();
        caso = creacion.crear(docente, new ServicioCreacionCaso.DatosCreacionCaso(datos.nombre(),
                datos.desarrolloSituacional(), datos.contexto(), datos.tiempoEstimadoMin(), datos.escenarios(), datos.rubrica())).id();
        publicacion.publicar(docente, caso);
        programacion = new ServicioGestionSesiones.DatosProgramacion(caso, espacio, AHORA, AHORA.plusSeconds(3600),
                30, ModoNavegacion.FLEXIBLE, "Aula 1", "Mensaje del docente", List.of(estudiante));
        var creada = sesiones.programar(docente, programacion);
        sesion = creada.id(); codigo = creada.convocatorias().getFirst().codigoAcceso();
    }

    // CP-NO-001/004: RF-20/21, CU-14. Entrega real sustituida por puerto de correo.
    @Test
    void convocatoriaCifradaSobreviveReconstruccionYSeEntregaUnaVez() {
        UUID id = ids().getFirst();
        String guardado = contenido(id);
        assertFalse(guardado.contains(codigo));
        assertTrue(cifrador.descifrar(guardado).contains(codigo));
        verifyNoInteractions(correo);
        var reconstruido = new RepositorioNotificacionesJdbc(jdbc,
                new CifradorNotificaciones("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="));
        var servicio = new ServicioEntregaNotificaciones(reconstruido, correo, identidades, reloj);
        new TransactionTemplate(gestor).executeWithoutResult(estadoTransaccion -> servicio.entregar(id));
        entrega.entregar(id);
        verify(correo, times(1)).enviar(eq(correoEstudiante), eq("Convocatoria academica"), argThat(
                texto -> texto.contains(codigo) && texto.contains("Conflicto") && texto.contains("Mensaje del docente")
                        && texto.contains("https://universidad.example/ingreso")));
        assertEquals("ENVIADA", estado(id));
        assertEquals("", contenido(id));
    }

    // CP-NO-005: fallos de SMTP no cambian datos academicos y se reintentan con espera.
    @Test
    void falloSmtpConservaConvocatoriaYReintentaSinFiltrarError() {
        UUID id = ids().getFirst();
        doThrow(new IllegalStateException("dato secreto " + codigo)).doNothing()
                .when(correo).enviar(anyString(), anyString(), anyString());
        entrega.entregar(id);
        assertEquals("PENDIENTE", estado(id));
        assertEquals(1, jdbc.sql("SELECT intentos FROM notificacion WHERE id_notificacion=?").param(id.toString()).query(Integer.class).single());
        assertEquals("ENVIO_FALLIDO", jdbc.sql("SELECT ultimo_error FROM notificacion WHERE id_notificacion=?").param(id.toString()).query(String.class).single());
        assertEquals("SIN_INICIAR", sesiones.consultar(docente, sesion).estado());
        entrega.entregar(id); // Todavia no corresponde el reintento.
        verify(correo, times(1)).enviar(anyString(), anyString(), anyString());
        when(reloj.instant()).thenReturn(AHORA.plusSeconds(30));
        entrega.entregar(id);
        assertEquals("ENVIADA", estado(id));
        verify(correo, times(2)).enviar(anyString(), anyString(), anyString());
    }

    // CP-NO-008: misma transaccion productora, sin red y sin aviso si hay rollback.
    @Test
    void rollbackDeProgramacionEliminaSesionYEntregaPendiente() {
        UUID[] creada = new UUID[1];
        new TransactionTemplate(gestor).executeWithoutResult(estadoTransaccion -> {
            creada[0] = sesiones.programar(docente, programacion).id();
            assertEquals(1, cantidad(creada[0]));
            estadoTransaccion.setRollbackOnly();
        });
        assertEquals(0, cantidad(creada[0]));
        assertThrows(NoEncontrado.class, () -> sesiones.consultar(docente, creada[0]));
        verifyNoInteractions(correo);
    }

    @Test
    void repetirEventoNoCreaOtraEntrega() {
        var evento = new SesionProgramada(UUID.randomUUID(), AHORA, estudiante, sesion,
                "Caso", "Mensaje", URI.create("https://universidad.example/ingreso"), "codigo-ficticio");
        new TransactionTemplate(gestor).executeWithoutResult(estadoTransaccion -> { eventos.publicar(evento); eventos.publicar(evento); });
        assertEquals(1, jdbc.sql("SELECT COUNT(*) FROM notificacion WHERE id_evento=?")
                .param(evento.idEvento().toString()).query(Integer.class).single());
    }

    @Test
    void trabajadoresConcurrentesNoEntreganDosVeces() throws Exception {
        UUID id = ids().getFirst();
        try (var trabajadores = Executors.newFixedThreadPool(2)) {
            var primero = trabajadores.submit(() -> entrega.entregar(id));
            var segundo = trabajadores.submit(() -> entrega.entregar(id));
            primero.get(10, TimeUnit.SECONDS); segundo.get(10, TimeUnit.SECONDS);
        }
        verify(correo, times(1)).enviar(anyString(), anyString(), anyString());
    }

    // CP-NO-003: retiro revoca acceso y cancela correo pendiente con codigo obsoleto.
    @Test
    void retiroYReingresoRotanCodigoYNotifican() {
        UUID inicial = ids().getFirst();
        sesiones.modificarConvocados(docente, sesion, new ServicioGestionSesiones.CambioConvocados(List.of(), List.of(estudiante)));
        assertEquals("CANCELADA", estado(inicial)); assertEquals("", contenido(inicial));
        var nuevo = sesiones.modificarConvocados(docente, sesion,
                new ServicioGestionSesiones.CambioConvocados(List.of(estudiante), List.of()));
        assertNotEquals(codigo, nuevo.convocatorias().getFirst().codigoAcceso());
        assertEquals(3, cantidad(sesion));
    }

    @Test
    void cambioInvalidoNoDejaAltasParciales() {
        UUID desconocido = UUID.randomUUID();
        int antes = cantidad(sesion);
        assertThrows(RuntimeException.class, () -> sesiones.modificarConvocados(docente, sesion,
                new ServicioGestionSesiones.CambioConvocados(List.of(desconocido), List.of(estudiante))));
        assertEquals(antes, cantidad(sesion));
        assertTrue(sesiones.consultar(docente, sesion).convocatorias().getFirst().autorizada());
    }

    // CP-NO-006: CU-24/25/33, nota automatica solo al finalizar.
    @Test
    void finalizarCalculaNotaYPublicaAvisoSinAnticiparJustificaciones() {
        sesiones.iniciar(docente, sesion);
        var intento = participacion.iniciar(estudiante, sesion, codigo, List.of(pregunta));
        assertTrue(avisos.listar(estudiante, 20, 0).isEmpty());
        participacion.responder(estudiante, intento.id(), pregunta, opcion);
        participacion.finalizar(estudiante, intento.id());
        assertEquals(0, evaluaciones.consultar(estudiante, intento.id()).resultado().calificacion().compareTo(new java.math.BigDecimal("5")));
        evaluaciones.evaluar(estudiante, intento.id()); // No duplica aviso ni calificacion.
        despachar();
        var recibidos = avisos.listar(estudiante, 20, 0);
        assertEquals(1, recibidos.size());
        assertEquals(intento.id(), recibidos.getFirst().idIntento());
        assertEquals("RESULTADO", recibidos.getFirst().tipo());
        assertFalse(recibidos.getFirst().mensaje().contains("Reconoce perspectivas"));
    }

    @Test
    void vencimientoCalculaCeroYGeneraAviso() {
        sesiones.iniciar(docente, sesion);
        var intento = participacion.iniciar(estudiante, sesion, codigo, List.of(pregunta));
        when(reloj.instant()).thenReturn(AHORA.plusSeconds(1800));
        assertEquals(1, participacion.finalizarVencidos(reloj.instant()));
        assertEquals(0, participacion.finalizarVencidos(reloj.instant()));
        assertEquals(0, evaluaciones.consultar(estudiante, intento.id()).resultado().calificacion().signum());
        despachar();
        assertEquals(1, avisos.listar(estudiante, 20, 0).size());
    }

    // CP-NO-007: propietario, roles, CSRF y consulta despues del cierre de sesion.
    @Test
    void valoracionSeNotificaYApiAislaAvisosDespuesDeCerrarSesion() throws Exception {
        sesiones.iniciar(docente, sesion);
        var intento = participacion.iniciar(estudiante, sesion, codigo, List.of(pregunta));
        participacion.finalizar(estudiante, intento.id());
        valoraciones.registrar(docente, intento.id(), "Sugerencia formativa");
        sesiones.finalizar(docente, sesion);
        despachar();
        assertEquals(2, avisos.listar(estudiante, 20, 0).size());
        UUID aviso = avisos.listar(estudiante, 20, 0).getFirst().id();
        mvc.perform(get("/api/notificaciones").with(httpBasic(correoEstudiante, codigo)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/notificaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notificaciones").with(user(docente.toString()).roles("DOCENTE"))).andExpect(status().isForbidden());
        UUID otro = UUID.randomUUID();
        mvc.perform(get("/api/notificaciones").with(user(otro.toString()).roles("ESTUDIANTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/notificaciones/{id}/leida", aviso).with(csrf()).with(user(otro.toString()).roles("ESTUDIANTE")))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/notificaciones/{id}/leida", aviso).with(user(estudiante.toString()).roles("ESTUDIANTE")))
                .andExpect(status().isForbidden());
        for (int vez = 0; vez < 2; vez++) mvc.perform(put("/api/notificaciones/{id}/leida", aviso)
                .with(csrf()).with(user(estudiante.toString()).roles("ESTUDIANTE"))).andExpect(status().isNoContent());
        assertTrue(avisos.listar(estudiante, 20, 0).stream().filter(a -> a.id().equals(aviso)).findFirst().orElseThrow().leida());
        mvc.perform(get("/api/evaluaciones/{id}/valoraciones", intento.id()).with(httpBasic(correoEstudiante, codigo)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].mensaje").value("Sugerencia formativa"));
    }

    @Test
    void soloResponsableValoraIntentoFinalizado() {
        sesiones.iniciar(docente, sesion);
        var intento = participacion.iniciar(estudiante, sesion, codigo, List.of(pregunta));
        assertThrows(Conflicto.class, () -> valoraciones.registrar(docente, intento.id(), "Aun no"));
        participacion.finalizar(estudiante, intento.id());
        assertThrows(AccesoDenegado.class, () -> valoraciones.registrar(estudiante, intento.id(), "No autorizado"));
        assertThrows(IllegalArgumentException.class, () -> valoraciones.registrar(docente, intento.id(), " "));
    }

    @Test
    void rollbackDeFinalizacionYValoracionNoPublicaAvisos() {
        sesiones.iniciar(docente, sesion);
        var intento = participacion.iniciar(estudiante, sesion, codigo, List.of(pregunta));
        new TransactionTemplate(gestor).executeWithoutResult(estadoTransaccion -> {
            participacion.finalizar(estudiante, intento.id());
            assertEquals(2, cantidad(sesion));
            estadoTransaccion.setRollbackOnly();
        });
        assertEquals("EN_PROGRESO", participacion.consultar(estudiante, intento.id()).estado());
        assertEquals(1, cantidad(sesion));
        assertThrows(NoEncontrado.class, () -> evaluaciones.consultar(estudiante, intento.id()));
        participacion.finalizar(estudiante, intento.id());
        new TransactionTemplate(gestor).executeWithoutResult(estadoTransaccion -> {
            valoraciones.registrar(docente, intento.id(), "Valoracion que se revierte");
            estadoTransaccion.setRollbackOnly();
        });
        assertTrue(valoraciones.consultar(estudiante, intento.id()).isEmpty());
        assertEquals(2, cantidad(sesion));
    }

    @Test
    void apiDeCambiosYValoracionesExigeRolCsrfYResponsable() throws Exception {
        var nuevo = identidades.registrarEstudiante(docente, "nuevo-" + UUID.randomUUID() + "@universidad.example", "Nuevo");
        String cambio = "{\"agregar\":[\"" + nuevo.id() + "\"],\"retirar\":[]}";
        mvc.perform(put("/api/sesiones/{id}/convocados", sesion).with(user(docente.toString()).roles("DOCENTE"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(cambio)).andExpect(status().isForbidden());
        mvc.perform(put("/api/sesiones/{id}/convocados", sesion).with(csrf()).with(user(estudiante.toString()).roles("ESTUDIANTE"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(cambio)).andExpect(status().isForbidden());
        mvc.perform(put("/api/sesiones/{id}/convocados", sesion).with(csrf()).with(user(docente.toString()).roles("DOCENTE"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(cambio)).andExpect(status().isOk())
                .andExpect(jsonPath("$.convocatorias[0].autorizada").value(true));
        sesiones.iniciar(docente, sesion);
        var intento = participacion.iniciar(estudiante, sesion, codigo, List.of(pregunta));
        participacion.finalizar(estudiante, intento.id());
        String mensaje = "{\"mensaje\":\"Valoracion formativa\"}";
        mvc.perform(post("/api/evaluaciones/{id}/valoraciones", intento.id()).with(csrf())
                .with(user(estudiante.toString()).roles("ESTUDIANTE"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(mensaje)).andExpect(status().isForbidden());
        mvc.perform(post("/api/evaluaciones/{id}/valoraciones", intento.id())
                .with(user(docente.toString()).roles("DOCENTE"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(mensaje)).andExpect(status().isForbidden());
        mvc.perform(post("/api/evaluaciones/{id}/valoraciones", intento.id()).with(csrf())
                .with(user(docente.toString()).roles("DOCENTE"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(mensaje)).andExpect(status().isCreated());
    }

    private void despachar() { for (UUID id : ids()) entrega.entregar(id); }
    private List<UUID> ids() {
        return jdbc.sql("SELECT id_notificacion FROM notificacion WHERE id_sesion=? ORDER BY id_notificacion")
                .param(sesion.toString()).query((fila, indice) -> UUID.fromString(fila.getString(1))).list();
    }
    private int cantidad(UUID idSesion) {
        return jdbc.sql("SELECT COUNT(*) FROM notificacion WHERE id_sesion=?").param(idSesion.toString()).query(Integer.class).single();
    }
    private String contenido(UUID id) {
        return jdbc.sql("SELECT mensaje FROM notificacion WHERE id_notificacion=?").param(id.toString()).query(String.class).single();
    }
    private String estado(UUID id) {
        return jdbc.sql("SELECT estado_envio FROM notificacion WHERE id_notificacion=?").param(id.toString()).query(String.class).single();
    }
}
