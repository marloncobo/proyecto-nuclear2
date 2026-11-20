package com.psicoapp.integracion;

import com.psicoapp.academic.application.FachadaAdministracion;
import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.application.ServicioUsuarios;
import com.psicoapp.identity.domain.Rol;
import com.psicoapp.shared.exceptions.Conflicto;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** RF-02/03/04, RN-10: contratos HTTP con autenticacion, servicios y JDBC reales. */
@SpringBootTest(properties = {
        "psicoapp.notificaciones.clave-cifrado=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "psicoapp.notificaciones.despacho-habilitado=false",
        "spring.datasource.url=jdbc:h2:mem:gestion-identidad;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "psicoapp.identidad.dominios-institucionales=example.test",
        "psicoapp.identidad.correo-administrador=", "psicoapp.identidad.clave-administrador=",
        "psicoapp.sesiones.cierre-automatico-habilitado=false"
})
@AutoConfigureMockMvc
class PruebaGestionIdentidadAcademica {
    private static final String CLAVE = "ClaveFicticia2026!";
    @Autowired MockMvc mvc;
    @Autowired ServicioUsuarios identidades;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired FachadaAdministracion academico;
    @Autowired JdbcClient jdbc;
    @Autowired PlatformTransactionManager gestor;
    private String correoAdministrador;
    private String correoDocente;
    private String correoEstudiante;
    private UUID administrador;
    private UUID docente;
    private UUID estudiante;

    @BeforeEach
    void preparar() {
        String sufijo = UUID.randomUUID().toString();
        correoAdministrador = "admin-" + sufijo + "@example.test";
        correoDocente = "docente-" + sufijo + "@example.test";
        correoEstudiante = "estudiante-" + sufijo + "@example.test";
        identidades.inicializarAdministrador(correoAdministrador, CLAVE);
        administrador = usuarios.buscarPorCorreo(correoAdministrador).orElseThrow().id();
        docente = identidades.registrarDocente(administrador, correoDocente, "Docente", CLAVE).id();
        estudiante = identidades.registrarEstudiante(docente, correoEstudiante, "Estudiante").id();
    }

    private MockHttpServletRequestBuilder comoAdministrador(MockHttpServletRequestBuilder solicitud) {
        return solicitud.with(httpBasic(correoAdministrador, CLAVE));
    }

    private MockHttpServletRequestBuilder escribir(MockHttpServletRequestBuilder solicitud, String contenido) {
        return comoAdministrador(solicitud).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(contenido);
    }

    @Test
    @DisplayName("RF-03: listado filtrado y detalle administrativo sin credenciales")
    void consultaUsuariosConFiltrosYDetalle() throws Exception {
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios").param("rol", "DOCENTE").param("activo", "true")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '%s')]".formatted(docente)).isNotEmpty())
                .andExpect(jsonPath("$[?(@.rol != 'DOCENTE')]").isEmpty())
                .andExpect(jsonPath("$..hashClave").isEmpty()).andExpect(jsonPath("$..clave").isEmpty());
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios/{id}", docente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.hashClave").doesNotExist()).andExpect(jsonPath("$.clave").doesNotExist());
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios").param("rol", "INVALIDO")))
                .andExpect(status().isBadRequest());
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios/{id}", UUID.randomUUID())))
                .andExpect(status().isNotFound());
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("RF-03/RN-10: busqueda por nombre/correo combinada con rol y estado")
    void filtraUsuariosEnJdbcPorNombreCorreoRolYEstado() throws Exception {
        String nombre = "Grupo-" + UUID.randomUUID();
        identidades.editarDocente(administrador, docente, correoDocente, nombre);
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios").param("busqueda", "  " + nombre.toUpperCase(java.util.Locale.ROOT) + "  ")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(docente.toString()));
        identidades.cambiarEstado(administrador, docente, false);
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios").param("busqueda", correoDocente.toUpperCase(java.util.Locale.ROOT))
                .param("rol", "DOCENTE").param("activo", "false")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].activo").value(false)).andExpect(jsonPath("$..hashClave").isEmpty());
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios").param("busqueda", correoDocente).param("activo", "true")))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios").param("busqueda", correoDocente).param("rol", "ESTUDIANTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        String correoOrdenPosterior = "z-" + UUID.randomUUID() + "@example.test";
        String correoOrdenAnterior = "a-" + UUID.randomUUID() + "@example.test";
        UUID posterior = identidades.registrarDocente(administrador, correoOrdenPosterior, "Nombre compartido", CLAVE).id();
        UUID anterior = identidades.registrarDocente(administrador, correoOrdenAnterior, "Nombre compartido", CLAVE).id();
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios").param("busqueda", "Nombre compartido")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(anterior.toString()))
                .andExpect(jsonPath("$[1].id").value(posterior.toString()));
    }

    @Test
    @DisplayName("RF-03/RN-10: consulta parametrizada trata comodines e intentos de SQL como texto")
    void buscaComodinesLiteralesSinInyectarSql() throws Exception {
        String sufijo = UUID.randomUUID().toString();
        identidades.editarDocente(administrador, docente, correoDocente, "Marca %_!" + sufijo);
        UUID soloPorcentaje = identidades.registrarDocente(administrador, "porcentaje-" + sufijo + "@example.test", "Marca %A!" + sufijo, CLAVE).id();
        UUID soloGuion = identidades.registrarDocente(administrador, "guion-" + sufijo + "@example.test", "Marca A_!" + sufijo, CLAVE).id();
        UUID soloExclamacion = identidades.registrarDocente(administrador, "exclamacion-" + sufijo + "@example.test", "Marca %_A" + sufijo, CLAVE).id();
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios").param("busqueda", "%_!" + sufijo)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(docente.toString()))
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(soloPorcentaje)).isEmpty())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(soloGuion)).isEmpty())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(soloExclamacion)).isEmpty());
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios").param("busqueda", "' OR 1=1 --")))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios").param("busqueda", "x".repeat(181))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/identidad/usuarios").param("busqueda", sufijo))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/identidad/usuarios").param("busqueda", sufijo).with(httpBasic(correoDocente, CLAVE)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF-03: edicion conserva rol, fecha y credenciales; rechaza duplicados")
    void editaDocenteSinPerderCredenciales() throws Exception {
        var anterior = usuarios.buscarPorId(docente).orElseThrow();
        String nuevoCorreo = "nuevo-" + correoDocente;
        mvc.perform(escribir(put("/api/identidad/docentes/{id}", docente),
                "{\"correo\":\"%s\",\"nombre\":\" Docente editado \"}".formatted(nuevoCorreo.toUpperCase(java.util.Locale.ROOT))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correo").value(nuevoCorreo))
                .andExpect(jsonPath("$.nombre").value("Docente editado"));
        var posterior = usuarios.buscarPorId(docente).orElseThrow();
        assertEquals(anterior.hashClave(), posterior.hashClave());
        assertEquals(anterior.fechaCreacion(), posterior.fechaCreacion());
        assertEquals(Rol.DOCENTE, posterior.rol());
        mvc.perform(get("/api/identidad/yo").with(httpBasic(nuevoCorreo, CLAVE))).andExpect(status().isOk());
        mvc.perform(get("/api/identidad/yo").with(httpBasic(correoDocente, CLAVE))).andExpect(status().isUnauthorized());
        mvc.perform(escribir(put("/api/identidad/docentes/{id}", docente),
                "{\"correo\":\"%s\",\"nombre\":\"Duplicado\"}".formatted(correoAdministrador)))
                .andExpect(status().isConflict());
        assertEquals(posterior, usuarios.buscarPorId(docente).orElseThrow());
        mvc.perform(escribir(put("/api/identidad/docentes/{id}", estudiante),
                "{\"correo\":\"%s\",\"nombre\":\"Cambio\"}".formatted(correoEstudiante)))
                .andExpect(status().isConflict());
        mvc.perform(escribir(put("/api/identidad/docentes/{id}", docente), "{\"correo\":\"invalido\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RF-02/03: desactivacion efectiva e idempotente sin perder credenciales")
    void desactivaYReactivaConProteccionPropia() throws Exception {
        var anterior = usuarios.buscarPorId(docente).orElseThrow();
        for (int intento = 0; intento < 2; intento++) {
            mvc.perform(escribir(put("/api/identidad/usuarios/{id}/estado", docente), "{\"activo\":false}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        }
        mvc.perform(get("/api/identidad/yo").with(httpBasic(correoDocente, CLAVE))).andExpect(status().isUnauthorized());
        mvc.perform(comoAdministrador(get("/api/identidad/usuarios").param("rol", "DOCENTE").param("activo", "false")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '%s')]".formatted(docente)).isNotEmpty());
        mvc.perform(escribir(put("/api/identidad/usuarios/{id}/estado", docente), "{\"activo\":true}"))
                .andExpect(status().isOk());
        assertEquals(anterior, usuarios.buscarPorId(docente).orElseThrow());
        mvc.perform(get("/api/identidad/yo").with(httpBasic(correoDocente, CLAVE))).andExpect(status().isOk());
        mvc.perform(escribir(put("/api/identidad/usuarios/{id}/estado", administrador), "{\"activo\":false}"))
                .andExpect(status().isConflict());
        mvc.perform(escribir(put("/api/identidad/usuarios/{id}/estado", docente), "{\"activo\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RF-03: consulta del catalogo de roles y del permiso compartido")
    void consultaRolesYPermisos() throws Exception {
        mvc.perform(comoAdministrador(get("/api/identidad/roles"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        for (Rol rol : Rol.values()) {
            mvc.perform(comoAdministrador(get("/api/identidad/roles/{rol}/creacion-casos", rol)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.habilitado").value(
                            rol == Rol.DOCENTE && identidades.consultarIdentidad(docente).puedeCrearCasos()));
        }
    }

    @Test
    @DisplayName("RF-04: busqueda exacta institucional no expone personal ni cuentas inactivas")
    void buscaEstudiantePorCorreo() throws Exception {
        mvc.perform(get("/api/identidad/estudiantes").param("correo", " " + correoEstudiante.toUpperCase(java.util.Locale.ROOT) + " ")
                .with(httpBasic(correoDocente, CLAVE))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(estudiante.toString()));
        for (String correo : new String[] {correoDocente, "ausente@example.test"}) {
            mvc.perform(get("/api/identidad/estudiantes").param("correo", correo).with(httpBasic(correoDocente, CLAVE)))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/identidad/estudiantes").param("correo", "externo@otro.test").with(httpBasic(correoDocente, CLAVE)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/identidad/estudiantes").with(httpBasic(correoDocente, CLAVE))).andExpect(status().isBadRequest());
        identidades.cambiarEstado(administrador, estudiante, false);
        mvc.perform(get("/api/identidad/estudiantes").param("correo", correoEstudiante).with(httpBasic(correoDocente, CLAVE)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RF-02/03/04: seguridad HTTP y autorizacion de servicios")
    void restringeRolesYExigeCsrf() throws Exception {
        for (String ruta : new String[] {"/api/identidad/usuarios", "/api/identidad/usuarios/" + docente,
                "/api/identidad/roles", "/api/identidad/roles/DOCENTE/creacion-casos"}) {
            mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
            mvc.perform(get(ruta).with(httpBasic(correoDocente, CLAVE))).andExpect(status().isForbidden());
            mvc.perform(get(ruta).with(user(estudiante.toString()).roles("ESTUDIANTE"))).andExpect(status().isForbidden());
        }
        for (String ruta : new String[] {"/api/identidad/docentes/" + docente, "/api/identidad/usuarios/" + docente + "/estado"}) {
            mvc.perform(put(ruta).with(httpBasic(correoDocente, CLAVE)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(comoAdministrador(put(ruta)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(comoAdministrador(get("/api/identidad/estudiantes").param("correo", correoEstudiante)))
                .andExpect(status().isForbidden());
        assertThrows(com.psicoapp.shared.exceptions.AccesoDenegado.class, () -> identidades.listarUsuarios(docente, null, null));
        assertThrows(com.psicoapp.shared.exceptions.AccesoDenegado.class, () -> identidades.editarDocente(docente, docente, correoDocente, "Cambio"));
        assertThrows(com.psicoapp.shared.exceptions.AccesoDenegado.class, () -> identidades.cambiarEstado(docente, estudiante, false));
    }

    @Test
    @DisplayName("RF-04: edicion valida, permisos, CSRF y espacio inexistente")
    void editaEspaciosDisponibles() throws Exception {
        UUID espacio = academico.crearEspacio(docente, "Original", "2026-2").id();
        String ruta = "/api/academico/espacios/" + espacio;
        mvc.perform(put(ruta).with(httpBasic(correoDocente, CLAVE)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\" Editado \",\"periodo\":\" 2027-1 \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Editado"));
        assertEquals("2027-1", academico.consultarEspacio(docente, espacio).periodo());
        mvc.perform(put(ruta).with(httpBasic(correoDocente, CLAVE)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"periodo\":\"2027-1\"}")).andExpect(status().isBadRequest());
        mvc.perform(put(ruta).with(httpBasic(correoDocente, CLAVE))).andExpect(status().isForbidden());
        mvc.perform(escribir(put(ruta), "{\"nombre\":\"Otro\",\"periodo\":\"2027-1\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/academico/espacios/{id}", UUID.randomUUID()).with(httpBasic(correoDocente, CLAVE))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Otro\",\"periodo\":\"2027-1\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RF-04: programacion concurrente bloquea edicion, incluso con sesion finalizada")
    void serializaProgramacionYEdicion() throws Exception {
        UUID espacio = academico.crearEspacio(docente, "Historico", "2026-2").id();
        UUID caso = UUID.randomUUID();
        jdbc.sql("INSERT INTO caso (id_caso,id_usuario_docente,nombre,desarrollo_situacional,contexto,estado) VALUES (?,?,?,?,?,?)")
                .params(caso.toString(), docente.toString(), "Caso", "Desarrollo", "Contexto", "PUBLICADO").update();
        TransactionTemplate transaccion = new TransactionTemplate(gestor);
        CountDownLatch iniciado = new CountDownLatch(1);
        try (ExecutorService ejecutor = Executors.newSingleThreadExecutor()) {
            Future<?>[] pendiente = new Future<?>[1];
            transaccion.executeWithoutResult(estado -> {
                academico.bloquearParaProgramacion(docente, espacio);
                pendiente[0] = ejecutor.submit(() -> {
                    iniciado.countDown();
                    return academico.editarEspacio(docente, espacio, "Cambio", "2027-1");
                });
                try {
                    assertTrue(iniciado.await(5, TimeUnit.SECONDS));
                    assertThrows(TimeoutException.class, () -> pendiente[0].get(200, TimeUnit.MILLISECONDS));
                } catch (InterruptedException excepcion) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(excepcion);
                }
                jdbc.sql("""
                        INSERT INTO sesion (id_sesion,id_caso,id_espacio,id_docente,fecha_inicio,fecha_fin,
                            duracion_maxima_min,modo_navegacion,estado)
                        VALUES (?,?,?,?, '2026-11-01 10:00:00', '2026-11-01 11:00:00',30,'FLEXIBLE','FINALIZADO')
                        """).params(UUID.randomUUID().toString(), caso.toString(), espacio.toString(), docente.toString()).update();
            });
            ExecutionException excepcion = assertThrows(ExecutionException.class, () -> pendiente[0].get(5, TimeUnit.SECONDS));
            assertInstanceOf(Conflicto.class, excepcion.getCause());
        }
        assertEquals("Historico", academico.consultarEspacio(docente, espacio).nombre());
        mvc.perform(put("/api/academico/espacios/{id}", espacio).with(httpBasic(correoDocente, CLAVE)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Cambio\",\"periodo\":\"2027-1\"}"))
                .andExpect(status().isConflict());
    }
}
