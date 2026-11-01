package com.psicoapp.identity.api;

import com.jayway.jsonpath.JsonPath;
import com.psicoapp.PsicoappApplication;
import com.psicoapp.academic.application.FachadaAdministracion;
import com.psicoapp.identity.application.ServicioUsuarios;
import com.psicoapp.identity.domain.Rol;
import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Solo se ejecuta con -Ppruebas-mysql. Cada ejecución crea y elimina SU esquema. */
@SpringBootTest
@AutoConfigureMockMvc
class PruebaPersistenciaMysqlIT {
    private static final String ESQUEMA = "psicoapp_pruebas_" + UUID.randomUUID().toString().replace("-", "");
    private static final String SERVIDOR = System.getenv().getOrDefault("PSICOAPP_PRUEBAS_MYSQL_URL", "jdbc:mysql://localhost:3306/");
    private static final String USUARIO_BD = System.getenv("PSICOAPP_PRUEBAS_MYSQL_USUARIO");
    private static final String CLAVE_BD = System.getenv("PSICOAPP_PRUEBAS_MYSQL_CLAVE");
    private static final String URL_BD = SERVIDOR + ESQUEMA;
    private static final String ADMINISTRADOR = "administrador@universidad.example";
    private static final String DOCENTE = "docente@universidad.example";
    private static final String OTRO_DOCENTE = "otro@universidad.example";
    private static final String CLAVE = "ClaveFicticia2026!";
    private static final String ESPACIOS = "/api/academico/espacios";
    private static boolean esquemaCreado;

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired ServicioUsuarios identidades;
    @Autowired PasswordEncoder claves;
    @Autowired PlatformTransactionManager transacciones;
    private UUID idAdministrador;

    @DynamicPropertySource
    static void prepararBaseAislada(DynamicPropertyRegistry propiedades) throws Exception {
        if (USUARIO_BD == null || CLAVE_BD == null) {
            throw new IllegalStateException("Configure PSICOAPP_PRUEBAS_MYSQL_USUARIO y PSICOAPP_PRUEBAS_MYSQL_CLAVE");
        }
        // La URL debe apuntar al servidor, sin seleccionar una base ni parámetros.
        if (!SERVIDOR.matches("jdbc:mysql://[^/]+/")) {
            throw new IllegalArgumentException("PSICOAPP_PRUEBAS_MYSQL_URL debe tener formato jdbc:mysql://host:puerto/");
        }
        try (var conexion = DriverManager.getConnection(SERVIDOR, USUARIO_BD, CLAVE_BD);
                var consulta = conexion.createStatement()) {
            consulta.executeUpdate("CREATE DATABASE " + ESQUEMA + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            esquemaCreado = true;
        }
        propiedades.add("spring.datasource.url", () -> URL_BD);
        propiedades.add("spring.datasource.username", () -> USUARIO_BD);
        propiedades.add("spring.datasource.password", () -> CLAVE_BD);
        propiedades.add("spring.sql.init.mode", () -> "always");
        propiedades.add("psicoapp.identidad.dominios-institucionales", () -> "universidad.example");
        propiedades.add("psicoapp.identidad.correo-administrador", () -> "");
        propiedades.add("psicoapp.identidad.clave-administrador", () -> "");
    }

    @AfterAll
    static void eliminarBasePropia() throws Exception {
        if (esquemaCreado && ESQUEMA.matches("psicoapp_pruebas_[a-f0-9]{32}")) {
            try (var conexion = DriverManager.getConnection(SERVIDOR, USUARIO_BD, CLAVE_BD);
                    var consulta = conexion.createStatement()) {
                consulta.executeUpdate("DROP DATABASE " + ESQUEMA);
                try (var resultado = consulta.executeQuery(
                        "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='" + ESQUEMA + "'")) {
                    assertFalse(resultado.next(), "El esquema temporal debe eliminarse al finalizar");
                }
            }
        }
    }

    @BeforeEach
    void prepararDatos() {
        assertEquals(ESQUEMA, jdbc.sql("SELECT DATABASE()").query(String.class).single());
        // Estas operaciones solo se hacen sobre el esquema aleatorio creado arriba.
        jdbc.sql("DELETE FROM rol_permiso").update();
        jdbc.sql("DELETE FROM usuario").update();
        jdbc.sql("DELETE FROM espacio_academico").update();
        identidades.inicializarAdministrador(ADMINISTRADOR, CLAVE);
        idAdministrador = UUID.fromString(jdbc.sql("SELECT id_usuario FROM usuario WHERE correo = ?")
                .param(ADMINISTRADOR).query(String.class).single());
        identidades.registrarDocente(idAdministrador, DOCENTE, "Docente", CLAVE);
        identidades.registrarDocente(idAdministrador, OTRO_DOCENTE, "Otro docente", CLAVE);
    }

    @Test
    @DisplayName("CP-ID-001/002: BCrypt, autenticación y duplicado normalizado con persistencia real")
    void registraDocenteYRechazaDuplicadosEnMysql() throws Exception {
        String datos = "{\"correo\":\"NUEVO@UNIVERSIDAD.EXAMPLE\",\"nombre\":\"Nuevo\",\"clave\":\"" + CLAVE + "\"}";
        String respuesta = modificar(post("/api/identidad/docentes"), ADMINISTRADOR, datos)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.correo").value("nuevo@universidad.example"))
                .andExpect(jsonPath("$.hashClave").doesNotExist()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(respuesta, "$.id");
        String resumen = jdbc.sql("SELECT contrasena_hash FROM usuario WHERE id_usuario = ?")
                .param(id).query(String.class).single();
        assertTrue(resumen.startsWith("$2"));
        assertTrue(claves.matches(CLAVE, resumen));
        var anterior = cuentas();
        modificar(post("/api/identidad/docentes"), ADMINISTRADOR, datos.replace("Nuevo", "Reemplazo"))
                .andExpect(status().isConflict());
        assertEquals(anterior, cuentas());
        mvc.perform(get("/api/identidad/yo").with(httpBasic("nuevo@universidad.example", CLAVE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @DisplayName("CP-ID-002: UNIQUE de MySQL impide duplicados incluso al omitir el servicio")
    void restriccionUniqueProtegeCorreo() {
        var anterior = cuentas();
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> jdbc.sql("""
                INSERT INTO usuario (id_usuario, nombre, correo, contrasena_hash, activo, fecha_creacion, id_rol)
                SELECT ?, 'Duplicado', UPPER(correo), contrasena_hash, activo, fecha_creacion, id_rol
                FROM usuario WHERE correo = ?
                """).params(UUID.randomUUID().toString(), DOCENTE).update());
        assertEquals(anterior, cuentas());
    }

    @ParameterizedTest
    @ValueSource(strings = {"nombreVacio", "nombreLargo", "correoInvalido", "claveCorta", "claveMultibyte"})
    @DisplayName("CP-ID-003: cada rechazo HTTP conserva los registros reales")
    void datosInvalidosNoPersisten(String variante) throws Exception {
        String nombre = variante.equals("nombreVacio") ? "" : variante.equals("nombreLargo") ? "A".repeat(121) : "Nuevo";
        String correo = variante.equals("correoInvalido") ? "sin-correo" : "nuevo@universidad.example";
        String clave = variante.equals("claveCorta") ? "corta" : variante.equals("claveMultibyte") ? "ñ".repeat(40) : CLAVE;
        var anterior = cuentas();
        modificar(post("/api/identidad/docentes"), ADMINISTRADOR,
                "{\"correo\":\"" + correo + "\",\"nombre\":\"" + nombre + "\",\"clave\":\"" + clave + "\"}")
                .andExpect(status().isBadRequest());
        assertEquals(anterior, cuentas());
    }

    @Test
    @DisplayName("CP-ID-005/006: cambios de rol persisten y el administrador conserva su rol propio")
    void cambiosDeRolPersisten() throws Exception {
        String idDocente = idCuenta(DOCENTE);
        for (String rol : new String[]{"ADMINISTRADOR", "DOCENTE"}) {
            modificar(put("/api/identidad/usuarios/{id}/rol", idDocente), ADMINISTRADOR,
                    "{\"rol\":\"" + rol + "\"}").andExpect(status().isOk());
            mvc.perform(get("/api/identidad/yo").with(httpBasic(DOCENTE, CLAVE)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value(rol));
            assertEquals(rol, jdbc.sql("SELECT r.nombre FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE id_usuario=?")
                    .param(idDocente).query(String.class).single());
        }
        var anterior = cuentas();
        modificar(put("/api/identidad/usuarios/{id}/rol", idAdministrador), ADMINISTRADOR,
                "{\"rol\":\"DOCENTE\"}").andExpect(status().isConflict());
        assertEquals(anterior, cuentas());
    }

    @Test
    @DisplayName("CP-ID-007/008: rol_permiso es idempotente, compartido y revocable")
    void permisosPersistenParaDosDocentes() throws Exception {
        for (boolean habilitado : new boolean[]{true, true, false, false}) {
            modificar(put("/api/identidad/roles/DOCENTE/creacion-casos"), ADMINISTRADOR,
                    "{\"habilitado\":" + habilitado + "}").andExpect(status().isNoContent());
            assertEquals(habilitado ? 1 : 0, jdbc.sql("SELECT COUNT(*) FROM rol_permiso").query(Integer.class).single());
            for (String correo : new String[]{DOCENTE, OTRO_DOCENTE}) {
                mvc.perform(get("/api/identidad/yo").with(httpBasic(correo, CLAVE)))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.puedeCrearCasos").value(habilitado));
            }
        }
        modificar(put("/api/identidad/roles/DOCENTE/creacion-casos"), DOCENTE,
                "{\"habilitado\":true}").andExpect(status().isForbidden());
        modificar(put("/api/identidad/roles/ESTUDIANTE/creacion-casos"), ADMINISTRADOR,
                "{\"habilitado\":true}").andExpect(status().isBadRequest());
        assertEquals(0, jdbc.sql("SELECT COUNT(*) FROM rol_permiso").query(Integer.class).single());
    }

    @Test
    @DisplayName("CP-ID-010/014: estudiante reutilizado conserva UUID y contraseña nula; sin convocatoria recibe 401")
    void reutilizaEstudiantePersistido() throws Exception {
        String id = null;
        for (String correo : new String[]{"alumno@universidad.example", "ALUMNO@UNIVERSIDAD.EXAMPLE"}) {
            String respuesta = modificar(post("/api/identidad/estudiantes"), DOCENTE,
                    "{\"correo\":\"" + correo + "\",\"nombre\":\"Alumno\"}")
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            String obtenido = JsonPath.read(respuesta, "$.id");
            if (id == null) id = obtenido;
            else assertEquals(id, obtenido);
        }
        assertEquals(1, jdbc.sql("SELECT COUNT(*) FROM usuario WHERE correo=? AND contrasena_hash IS NULL")
                .param("alumno@universidad.example").query(Integer.class).single());
        assertEquals(4, cuentas().size());
        mvc.perform(get("/api/identidad/yo").with(httpBasic("alumno@universidad.example", "CodigoNoVerificable")))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"alumno@externo.example", "alumno@universidad.example.externo.example", DOCENTE})
    @DisplayName("CP-ID-011: dominios externos y correo de personal no alteran MySQL")
    void rechazaCorreoEstudianteSinPersistir(String correo) throws Exception {
        var anterior = cuentas();
        modificar(post("/api/identidad/estudiantes"), DOCENTE,
                "{\"correo\":\"" + correo + "\",\"nombre\":\"Alumno\"}")
                .andExpect(status().is(correo.equals(DOCENTE) ? 409 : 400));
        assertEquals(anterior, cuentas());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DOCENTE", "ADMINISTRADOR"})
    @DisplayName("CP-ID-012: promoción inválida no cambia el registro estudiantil")
    void promocionInvalidaNoPersiste(String rol) throws Exception {
        var estudiante = identidades.registrarEstudiante(UUID.fromString(idCuenta(DOCENTE)), "alumno@universidad.example", "Alumno");
        var anterior = cuentas();
        modificar(put("/api/identidad/usuarios/{id}/rol", estudiante.id()), ADMINISTRADOR,
                "{\"rol\":\"" + rol + "\"}").andExpect(status().isBadRequest());
        assertEquals(anterior, cuentas());
    }

    @Test
    @DisplayName("CP-ID-004/013: alta por docente o sin CSRF no escribe en MySQL")
    void altasNoAutorizadasNoPersisten() throws Exception {
        var anterior = cuentas();
        String datos = "{\"correo\":\"nuevo@universidad.example\",\"nombre\":\"Nuevo\",\"clave\":\"" + CLAVE + "\"}";
        modificar(post("/api/identidad/docentes"), DOCENTE, datos).andExpect(status().isForbidden());
        mvc.perform(post("/api/identidad/docentes").with(httpBasic(ADMINISTRADOR, CLAVE))
                .contentType(MediaType.APPLICATION_JSON).content(datos)).andExpect(status().isForbidden());
        assertEquals(anterior, cuentas());
    }

    @Test
    @DisplayName("CP-AC-001/003/006: máximos exactos persisten, excesos no y UUID inexistente devuelve 404")
    void espaciosPersistenConSusLimites() throws Exception {
        String nombre = "A".repeat(150);
        String periodo = "B".repeat(30);
        String respuesta = modificar(post(ESPACIOS), DOCENTE, datosEspacio(nombre, periodo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(respuesta, "$.id");
        mvc.perform(get(ESPACIOS + "/{id}", id).with(httpBasic(DOCENTE, CLAVE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value(nombre))
                .andExpect(jsonPath("$.periodo").value(periodo));
        assertEquals(150, jdbc.sql("SELECT CHAR_LENGTH(nombre) FROM espacio_academico WHERE id_espacio=?")
                .param(id).query(Integer.class).single());
        assertEquals(30, jdbc.sql("SELECT CHAR_LENGTH(periodo) FROM espacio_academico WHERE id_espacio=?")
                .param(id).query(Integer.class).single());
        var anterior = catalogo();
        modificar(post(ESPACIOS), DOCENTE, datosEspacio(nombre + "A", periodo)).andExpect(status().isBadRequest());
        modificar(post(ESPACIOS), DOCENTE, datosEspacio(nombre, periodo + "B")).andExpect(status().isBadRequest());
        mvc.perform(get(ESPACIOS + "/{id}", UUID.randomUUID()).with(httpBasic(DOCENTE, CLAVE)))
                .andExpect(status().isNotFound());
        assertEquals(anterior, catalogo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"nombre\":\"\",\"periodo\":\"2026-2\"}", "{\"nombre\":\" \",\"periodo\":\"2026-2\"}",
            "{\"periodo\":\"2026-2\"}", "{\"nombre\":\"Valido\",\"periodo\":\"\"}",
            "{\"nombre\":\"Valido\",\"periodo\":\" \"}", "{\"nombre\":\"Valido\"}"})
    @DisplayName("CP-AC-002: espacios incompletos no generan registros parciales")
    void espaciosInvalidosNoPersisten(String datos) throws Exception {
        modificar(post(ESPACIOS), DOCENTE, datosEspacio("Existente", "2026-2")).andExpect(status().isCreated());
        var anterior = catalogo();
        modificar(post(ESPACIOS), DOCENTE, datos).andExpect(status().isBadRequest());
        assertEquals(anterior, catalogo());
    }

    @Test
    @DisplayName("CP-AC-004/005: catálogo vacío, compartido y ordenado por periodo, nombre e UUID en SQL")
    void catalogoCompartidoRespetaOrdenMysql() throws Exception {
        mvc.perform(get(ESPACIOS).with(httpBasic(DOCENTE, CLAVE)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        String[] identificadores = {"00000000-0000-0000-0000-000000000004", "00000000-0000-0000-0000-000000000003",
                "00000000-0000-0000-0000-000000000002", "00000000-0000-0000-0000-000000000001"};
        String[] nombres = {"B", "Z", "A", "A"};
        String[] periodos = {"2026-2", "2026-1", "2026-2", "2026-2"};
        for (int indice = 0; indice < identificadores.length; indice++) {
            jdbc.sql("INSERT INTO espacio_academico (id_espacio,nombre,periodo) VALUES (?,?,?)")
                    .params(identificadores[indice], nombres[indice], periodos[indice]).update();
        }
        var anterior = catalogo();
        for (String correo : new String[]{DOCENTE, OTRO_DOCENTE}) {
            mvc.perform(get(ESPACIOS).with(httpBasic(correo, CLAVE))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$[0].id").value(identificadores[1]))
                    .andExpect(jsonPath("$[1].id").value(identificadores[3]))
                    .andExpect(jsonPath("$[2].id").value(identificadores[2]))
                    .andExpect(jsonPath("$[3].id").value(identificadores[0]));
        }
        assertEquals(anterior, catalogo());
    }

    @Test
    @DisplayName("CP-AC-007: administrador y anónimo no alteran el catálogo real")
    void accesoNoAutorizadoNoAlteraCatalogo() throws Exception {
        String id = JsonPath.read(modificar(post(ESPACIOS), DOCENTE, datosEspacio("Existente", "2026-2"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        var anterior = catalogo();
        mvc.perform(get(ESPACIOS).with(httpBasic(ADMINISTRADOR, CLAVE))).andExpect(status().isForbidden());
        mvc.perform(get(ESPACIOS + "/{id}", id).with(httpBasic(ADMINISTRADOR, CLAVE))).andExpect(status().isForbidden());
        modificar(post(ESPACIOS), ADMINISTRADOR, datosEspacio("Prohibido", "2026-2")).andExpect(status().isForbidden());
        mvc.perform(get(ESPACIOS)).andExpect(status().isUnauthorized());
        assertEquals(anterior, catalogo());
    }

    @Test
    @DisplayName("CP-AC-001/CP-ID-007: espacio y permiso sobreviven cerrar y arrancar el backend")
    void datosSobrevivenReinicioDelBackend() {
        UUID idEspacio;
        try (var primera = arrancarBackend()) {
            var servicio = primera.getBean(ServicioUsuarios.class);
            servicio.configurarCreacionCasos(idAdministrador, Rol.DOCENTE, true);
            idEspacio = primera.getBean(FachadaAdministracion.class)
                    .crearEspacio(UUID.fromString(idCuenta(DOCENTE)), "Persistente", "2026-2").id();
        }
        try (var segunda = arrancarBackend()) {
            var servicio = segunda.getBean(ServicioUsuarios.class);
            assertTrue(servicio.consultarIdentidad(UUID.fromString(idCuenta(DOCENTE))).puedeCrearCasos());
            var espacio = segunda.getBean(FachadaAdministracion.class)
                    .consultarEspacio(UUID.fromString(idCuenta(DOCENTE)), idEspacio);
            assertEquals("Persistente", espacio.nombre());
            assertEquals("2026-2", espacio.periodo());
        }
    }

    private ConfigurableApplicationContext arrancarBackend() {
        var entorno = new StandardEnvironment();
        entorno.getPropertySources().addFirst(new MapPropertySource("pruebasMysql", Map.of(
                "spring.datasource.url", URL_BD, "spring.datasource.username", USUARIO_BD,
                "spring.datasource.password", CLAVE_BD, "server.port", "0",
                "psicoapp.identidad.dominios-institucionales", "universidad.example",
                "psicoapp.identidad.correo-administrador", "", "psicoapp.identidad.clave-administrador", "",
                "spring.sql.init.mode", "always")));
        var aplicacion = new SpringApplication(PsicoappApplication.class);
        aplicacion.setEnvironment(entorno);
        return aplicacion.run();
    }

    @Test
    @DisplayName("Persistencia: un fallo transaccional revierte el alta y el permiso del rol")
    void falloTransaccionalRevierteEscrituras() {
        var anterior = cuentas();
        var transaccion = new TransactionTemplate(transacciones);
        assertThrows(IllegalStateException.class, () -> transaccion.executeWithoutResult(estado -> {
            identidades.registrarDocente(idAdministrador, "transaccion@universidad.example", "Temporal", CLAVE);
            identidades.configurarCreacionCasos(idAdministrador, Rol.DOCENTE, true);
            assertEquals(4, cuentas().size());
            assertEquals(1, jdbc.sql("SELECT COUNT(*) FROM rol_permiso").query(Integer.class).single());
            throw new IllegalStateException("Fallo controlado para verificar rollback");
        }));
        assertEquals(anterior, cuentas());
        assertEquals(0, jdbc.sql("SELECT COUNT(*) FROM rol_permiso").query(Integer.class).single());
    }

    // Se omiten hashes en las instantáneas y en los mensajes de aserción.
    private java.util.List<Map<String, Object>> cuentas() {
        return jdbc.sql("SELECT id_usuario,nombre,correo,id_rol,activo FROM usuario ORDER BY id_usuario").query().listOfRows();
    }

    private java.util.List<Map<String, Object>> catalogo() {
        return jdbc.sql("SELECT * FROM espacio_academico ORDER BY id_espacio").query().listOfRows();
    }

    private String idCuenta(String correo) {
        return jdbc.sql("SELECT id_usuario FROM usuario WHERE correo=?").param(correo).query(String.class).single();
    }

    private ResultActions modificar(MockHttpServletRequestBuilder solicitud, String correo, String datos) throws Exception {
        var respuesta = mvc.perform(get("/api/identidad/csrf").with(httpBasic(correo, CLAVE)))
                .andExpect(status().isOk()).andReturn().getResponse();
        var cookie = respuesta.getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        String cabecera = JsonPath.read(respuesta.getContentAsString(), "$.nombreCabecera");
        String token = JsonPath.read(respuesta.getContentAsString(), "$.token");
        return mvc.perform(solicitud.with(httpBasic(correo, CLAVE)).cookie(cookie).header(cabecera, token)
                .contentType(MediaType.APPLICATION_JSON).content(datos));
    }

    private String datosEspacio(String nombre, String periodo) {
        return "{\"nombre\":\"" + nombre + "\",\"periodo\":\"" + periodo + "\"}";
    }
}
