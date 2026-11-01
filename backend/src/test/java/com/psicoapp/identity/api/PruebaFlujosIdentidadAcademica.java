package com.psicoapp.identity.api;

import com.jayway.jsonpath.JsonPath;
import com.psicoapp.academic.api.ControladorEspaciosAcademicos;
import com.psicoapp.academic.application.FachadaAdministracion;
import com.psicoapp.academic.application.RepositorioEspaciosAcademicos;
import com.psicoapp.academic.domain.EspacioAcademico;
import com.psicoapp.identity.application.PermisosRol;
import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.application.ServicioUsuarios;
import com.psicoapp.identity.domain.Rol;
import com.psicoapp.identity.domain.Usuario;
import com.psicoapp.identity.infrastructure.ConfiguracionIdentidad;
import com.psicoapp.identity.infrastructure.ProveedorAutenticacionIdentidad;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Flujos HTTP con servicios reales y dobles de almacenamiento; no comprueba JDBC/MySQL. */
@WebMvcTest(controllers = {ControladorIdentidad.class, ControladorEspaciosAcademicos.class}, properties = {
        "psicoapp.identidad.correo-administrador=", "psicoapp.identidad.clave-administrador=",
        "psicoapp.identidad.dominios-institucionales=universidad.example"
})
@Import({ConfiguracionIdentidad.class, ProveedorAutenticacionIdentidad.class,
        ServicioUsuarios.class, FachadaAdministracion.class})
class PruebaFlujosIdentidadAcademica {
    private static final String CLAVE = "ClaveFicticia2026!";
    private static final String ADMINISTRADOR = "administrador@universidad.example";
    private static final String DOCENTE = "docente@universidad.example";
    private static final String OTRO_DOCENTE = "otro.docente@universidad.example";
    private static final String ESTUDIANTE = "estudiante@universidad.example";
    private static final String ESPACIOS = "/api/academico/espacios";
    private static final String PERMISOS = "/api/identidad/roles/DOCENTE/creacion-casos";

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder claves;
    @MockitoBean RepositorioUsuarios usuarios;
    @MockitoBean PermisosRol permisos;
    @MockitoBean RepositorioEspaciosAcademicos espacios;
    private final Map<UUID, Usuario> cuentas = new LinkedHashMap<>();
    private final Map<UUID, EspacioAcademico> catalogo = new LinkedHashMap<>();
    private final EnumSet<Rol> rolesConPermiso = EnumSet.noneOf(Rol.class);

    @BeforeEach
    void preparar() {
        cuentas.clear();
        catalogo.clear();
        rolesConPermiso.clear();
        // Solo los puertos de almacenamiento se sustituyen. La autenticación, las
        // validaciones, las reglas de negocio y los controladores son los reales.
        when(usuarios.buscarPorId(any())).thenAnswer(consulta -> Optional.ofNullable(cuentas.get(consulta.getArgument(0))));
        when(usuarios.buscarPorCorreo(anyString())).thenAnswer(consulta -> cuentas.values().stream()
                .filter(usuario -> usuario.correo().equals(consulta.getArgument(0))).findFirst());
        doAnswer(operacion -> {
            Usuario usuario = operacion.getArgument(0);
            cuentas.put(usuario.id(), usuario);
            return null;
        }).when(usuarios).guardar(any());
        when(permisos.puedeCrearCasos(any())).thenAnswer(consulta -> rolesConPermiso.contains(consulta.getArgument(0)));
        doAnswer(operacion -> {
            Rol rol = operacion.getArgument(0);
            if (operacion.<Boolean>getArgument(1)) rolesConPermiso.add(rol);
            else rolesConPermiso.remove(rol);
            return null;
        }).when(permisos).configurarCreacionCasos(any(), anyBoolean());
        when(espacios.buscarPorId(any())).thenAnswer(consulta -> Optional.ofNullable(catalogo.get(consulta.getArgument(0))));
        when(espacios.listarTodos()).thenAnswer(consulta -> new ArrayList<>(catalogo.values()));
        doAnswer(operacion -> {
            EspacioAcademico espacio = operacion.getArgument(0);
            catalogo.put(espacio.id(), espacio);
            return null;
        }).when(espacios).guardar(any());
        String resumen = claves.encode(CLAVE);
        agregarCuenta(ADMINISTRADOR, Rol.ADMINISTRADOR, resumen);
        agregarCuenta(DOCENTE, Rol.DOCENTE, resumen);
        agregarCuenta(OTRO_DOCENTE, Rol.DOCENTE, resumen);
        agregarCuenta(ESTUDIANTE, Rol.ESTUDIANTE, null);
    }

    @Test
    @DisplayName("CP-ID-001: alta docente, BCrypt e inicio de sesión con la cuenta creada")
    void registraDocenteYAutenticaCuentaCreada() throws Exception {
        String correo = "nuevo@universidad.example";
        String respuesta = modificar(post("/api/identidad/docentes"), ADMINISTRADOR,
                datosDocente(correo.toUpperCase(), "Docente nuevo", CLAVE))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.correo").value(correo))
                .andExpect(jsonPath("$.rol").value("DOCENTE"))
                .andExpect(jsonPath("$.clave").doesNotExist()).andExpect(jsonPath("$.hashClave").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(respuesta, "$.id");
        consultarIdentidad(correo).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.rol").value("DOCENTE"));
        assertEquals(5, cuentas.size());
        Usuario guardado = cuentas.get(UUID.fromString(id));
        assertTrue(guardado.activo());
        assertTrue(claves.matches(CLAVE, guardado.hashClave()));
        assertNotEquals(CLAVE, guardado.hashClave());
    }

    @Test
    @DisplayName("CP-ID-002: duplicado en mayúsculas devuelve 409 y conserva cuenta y credenciales")
    void rechazaDuplicadoSinModificarCuenta() throws Exception {
        var inicial = Map.copyOf(cuentas);
        modificar(post("/api/identidad/docentes"), ADMINISTRADOR,
                datosDocente(DOCENTE.toUpperCase(), "Reemplazo", "OtraClaveFicticia!"))
                .andExpect(status().isConflict());
        assertEquals(inicial, cuentas);
        consultarIdentidad(DOCENTE).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cuenta(DOCENTE).id().toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"nombreVacio", "nombreLargo", "correoInvalido", "claveCorta", "claveMultibyte"})
    @DisplayName("CP-ID-003: cada variante inválida devuelve 400 sin guardar cuentas")
    void rechazaDatosDocenteInvalidosPorHttp(String variante) throws Exception {
        String nombre = switch (variante) {
            case "nombreVacio" -> "";
            case "nombreLargo" -> "A".repeat(121);
            default -> "Docente";
        };
        String correo = variante.equals("correoInvalido") ? "sin-correo" : "nuevo@universidad.example";
        String clave = switch (variante) {
            case "claveCorta" -> "corta";
            case "claveMultibyte" -> "ñ".repeat(40);
            default -> CLAVE;
        };
        var inicial = Map.copyOf(cuentas);
        modificar(post("/api/identidad/docentes"), ADMINISTRADOR, datosDocente(correo, nombre, clave))
                .andExpect(status().isBadRequest());
        assertEquals(inicial, cuentas);
        verify(usuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("CP-ID-004/008: docente autenticado no crea docentes ni configura permisos")
    void docenteNoAdministraIdentidadesNiPermisos() throws Exception {
        var inicial = Map.copyOf(cuentas);
        modificar(post("/api/identidad/docentes"), DOCENTE, datosDocente("nuevo@universidad.example", "Nuevo", CLAVE))
                .andExpect(status().isForbidden());
        modificar(put(PERMISOS), DOCENTE, "{\"habilitado\":true}").andExpect(status().isForbidden());
        assertEquals(inicial, cuentas);
        verify(usuarios, never()).guardar(any());
        verify(permisos, never()).configurarCreacionCasos(any(), anyBoolean());
    }

    @Test
    @DisplayName("CP-ID-005: promoción y restitución cambian la autorización de la siguiente solicitud")
    void cambiaRolYRestituyeAutorizacion() throws Exception {
        String ruta = "/api/identidad/usuarios/" + cuenta(DOCENTE).id() + "/rol";
        modificar(put(ruta), ADMINISTRADOR, "{\"rol\":\"ADMINISTRADOR\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(cuenta(DOCENTE).id().toString()))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
        consultarIdentidad(DOCENTE).andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
        modificar(put(PERMISOS), DOCENTE, "{\"habilitado\":true}").andExpect(status().isNoContent());
        modificar(put(ruta), ADMINISTRADOR, "{\"rol\":\"DOCENTE\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("DOCENTE"));
        consultarIdentidad(DOCENTE).andExpect(jsonPath("$.rol").value("DOCENTE"));
        modificar(put(PERMISOS), DOCENTE, "{\"habilitado\":false}").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CP-ID-006: administrador no retira su propio rol y conserva acceso")
    void rechazaRetirarRolPropioPorHttp() throws Exception {
        var inicial = Map.copyOf(cuentas);
        modificar(put("/api/identidad/usuarios/{id}/rol", cuenta(ADMINISTRADOR).id()), ADMINISTRADOR,
                "{\"rol\":\"DOCENTE\"}").andExpect(status().isConflict());
        consultarIdentidad(ADMINISTRADOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
        assertEquals(inicial, cuentas);
    }

    @Test
    @DisplayName("CP-ID-007: concesión y revocación del permiso afectan a dos docentes")
    void permisoDelRolAfectaDosDocentes() throws Exception {
        for (boolean habilitado : new boolean[]{false, true, false}) {
            modificar(put(PERMISOS), ADMINISTRADOR, "{\"habilitado\":" + habilitado + "}")
                    .andExpect(status().isNoContent());
            for (String correo : new String[]{DOCENTE, OTRO_DOCENTE}) {
                consultarIdentidad(correo).andExpect(status().isOk())
                        .andExpect(jsonPath("$.puedeCrearCasos").value(habilitado));
            }
        }
        assertTrue(rolesConPermiso.isEmpty());
    }

    @Test
    @DisplayName("CP-ID-008: no se concede CREAR_CASOS al rol estudiante por HTTP")
    void rechazaPermisoParaEstudiantePorHttp() throws Exception {
        modificar(put("/api/identidad/roles/ESTUDIANTE/creacion-casos"), ADMINISTRADOR,
                "{\"habilitado\":true}").andExpect(status().isBadRequest());
        assertTrue(rolesConPermiso.isEmpty());
        verify(permisos, never()).configurarCreacionCasos(any(), anyBoolean());
    }

    @ParameterizedTest
    @ValueSource(strings = {ADMINISTRADOR, DOCENTE})
    @DisplayName("CP-ID-009: personal consulta identidad completa sin exponer credenciales")
    void autenticaPersonalPorHttp(String correo) throws Exception {
        Usuario usuario = cuenta(correo);
        consultarIdentidad(correo).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.id().toString()))
                .andExpect(jsonPath("$.correo").value(correo)).andExpect(jsonPath("$.nombre").value(usuario.nombre()))
                .andExpect(jsonPath("$.rol").value(usuario.rol().name()))
                .andExpect(jsonPath("$.puedeCrearCasos").value(false))
                .andExpect(jsonPath("$.clave").doesNotExist()).andExpect(jsonPath("$.hashClave").doesNotExist());
        mvc.perform(get("/api/identidad/yo").with(httpBasic(correo, "incorrecta")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/identidad/yo")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CP-ID-010: alta y reutilización en mayúsculas devuelven 201 y el mismo UUID")
    void registraYReutilizaEstudiantePorHttp() throws Exception {
        String id = null;
        for (String correo : new String[]{"nuevo@universidad.example", "NUEVO@UNIVERSIDAD.EXAMPLE"}) {
            String respuesta = modificar(post("/api/identidad/estudiantes"), DOCENTE,
                    "{\"correo\":\"" + correo + "\",\"nombre\":\"Estudiante nuevo\"}")
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("ESTUDIANTE"))
                    .andExpect(jsonPath("$.correo").value("nuevo@universidad.example"))
                    .andExpect(jsonPath("$.clave").doesNotExist()).andExpect(jsonPath("$.hashClave").doesNotExist())
                    .andReturn().getResponse().getContentAsString();
            String obtenido = JsonPath.read(respuesta, "$.id");
            if (id == null) id = obtenido;
            else assertEquals(id, obtenido);
        }
        assertEquals(5, cuentas.size());
        assertNull(cuentas.get(UUID.fromString(id)).hashClave());
        verify(usuarios, times(1)).guardar(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"alumno@externo.example", "alumno@universidad.example.externo.example", DOCENTE})
    @DisplayName("CP-ID-011: correo externo o del personal no registra estudiantes")
    void rechazaCorreoEstudiantilPorHttp(String correo) throws Exception {
        var inicial = Map.copyOf(cuentas);
        modificar(post("/api/identidad/estudiantes"), DOCENTE,
                "{\"correo\":\"" + correo + "\",\"nombre\":\"Prueba\"}")
                .andExpect(status().is(correo.equals(DOCENTE) ? 409 : 400));
        assertEquals(inicial, cuentas);
        verify(usuarios, never()).guardar(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DOCENTE", "ADMINISTRADOR"})
    @DisplayName("CP-ID-012: estudiante sin contraseña no puede promoverse a personal")
    void rechazaPromoverEstudiantePorHttp(String rol) throws Exception {
        var inicial = Map.copyOf(cuentas);
        modificar(put("/api/identidad/usuarios/{id}/rol", cuenta(ESTUDIANTE).id()), ADMINISTRADOR,
                "{\"rol\":\"" + rol + "\"}").andExpect(status().isBadRequest());
        assertEquals(inicial, cuentas);
        verify(usuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("CP-ID-013: alta docente sin CSRF es 403; cookie y token reales permiten el alta")
    void altaDocenteRequiereCsrf() throws Exception {
        String datos = datosDocente("nuevo@universidad.example", "Nuevo", CLAVE);
        mvc.perform(post("/api/identidad/docentes").with(httpBasic(ADMINISTRADOR, CLAVE))
                        .contentType(MediaType.APPLICATION_JSON).content(datos))
                .andExpect(status().isForbidden());
        verify(usuarios, never()).guardar(any());
        assertEquals(4, cuentas.size());
        modificar(post("/api/identidad/docentes"), ADMINISTRADOR, datos).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CP-ID-014: identidad estudiantil sin adaptador de convocatorias no autentica")
    void rechazaEstudianteSinAdaptadorPorHttp() throws Exception {
        var inicial = Map.copyOf(cuentas);
        mvc.perform(get("/api/identidad/yo").with(httpBasic(ESTUDIANTE, "CodigoNoVerificable2026")))
                .andExpect(status().isUnauthorized());
        assertEquals(inicial, cuentas);
    }

    @Test
    @DisplayName("CP-AC-001/006: crea y consulta espacio; UUID ausente devuelve 404 sin crear datos")
    void creaConsultaYRechazaEspacioAusente() throws Exception {
        String respuesta = modificar(post(ESPACIOS), DOCENTE, datosEspacio("Psicologia", "2026-2"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nombre").value("Psicologia"))
                .andExpect(jsonPath("$.periodo").value("2026-2"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(respuesta, "$.id");
        mvc.perform(get(ESPACIOS + "/{id}", id).with(httpBasic(DOCENTE, CLAVE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Psicologia")).andExpect(jsonPath("$.periodo").value("2026-2"));
        var inicial = Map.copyOf(catalogo);
        mvc.perform(get(ESPACIOS + "/{id}", UUID.randomUUID()).with(httpBasic(DOCENTE, CLAVE)))
                .andExpect(status().isNotFound());
        assertEquals(inicial, catalogo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"nombre\":\"\",\"periodo\":\"2026-2\"}",
            "{\"nombre\":\" \",\"periodo\":\"2026-2\"}", "{\"periodo\":\"2026-2\"}",
            "{\"nombre\":\"Psicologia\",\"periodo\":\"\"}", "{\"nombre\":\"Psicologia\",\"periodo\":\" \"}",
            "{\"nombre\":\"Psicologia\"}"})
    @DisplayName("CP-AC-002: cada campo vacío, blanco u omitido devuelve 400 sin guardar")
    void rechazaEspaciosIncompletosPorHttp(String datos) throws Exception {
        modificar(post(ESPACIOS), DOCENTE, datosEspacio("Existente", "2026-2")).andExpect(status().isCreated());
        var inicial = Map.copyOf(catalogo);
        modificar(post(ESPACIOS), DOCENTE, datos).andExpect(status().isBadRequest());
        assertEquals(inicial, catalogo);
        mvc.perform(get(ESPACIOS).with(httpBasic(DOCENTE, CLAVE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(inicial.keySet().iterator().next().toString()));
        verify(espacios, times(1)).guardar(any());
    }

    @Test
    @DisplayName("CP-AC-003: acepta máximos exactos de 150/30 y rechaza 151/31")
    void validaLongitudesExactasPorHttp() throws Exception {
        String nombre = "A".repeat(150);
        String periodo = "B".repeat(30);
        modificar(post(ESPACIOS), DOCENTE, datosEspacio(nombre, periodo)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value(nombre)).andExpect(jsonPath("$.periodo").value(periodo));
        var inicial = Map.copyOf(catalogo);
        modificar(post(ESPACIOS), DOCENTE, datosEspacio(nombre + "A", periodo)).andExpect(status().isBadRequest());
        modificar(post(ESPACIOS), DOCENTE, datosEspacio(nombre, periodo + "B")).andExpect(status().isBadRequest());
        assertEquals(inicial, catalogo);
        verify(espacios, times(1)).guardar(any());
    }

    @Test
    @DisplayName("CP-AC-004: catálogo vacío devuelve 200 y [] sin crear datos")
    void consultaCatalogoVacioPorHttp() throws Exception {
        mvc.perform(get(ESPACIOS).with(httpBasic(DOCENTE, CLAVE)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        assertTrue(catalogo.isEmpty());
        verify(espacios, never()).guardar(any());
    }

    @Test
    @DisplayName("CP-AC-005: dos docentes consultan los mismos espacios sin cambiar el catálogo")
    void catalogoEsCompartidoEntreDocentes() throws Exception {
        modificar(post(ESPACIOS), DOCENTE, datosEspacio("B", "2026-2")).andExpect(status().isCreated());
        modificar(post(ESPACIOS), DOCENTE, datosEspacio("Z", "2026-1")).andExpect(status().isCreated());
        modificar(post(ESPACIOS), OTRO_DOCENTE, datosEspacio("A", "2026-2")).andExpect(status().isCreated());
        var inicial = Map.copyOf(catalogo);
        String primera = null;
        for (String correo : new String[]{DOCENTE, OTRO_DOCENTE}) {
            String respuesta = mvc.perform(get(ESPACIOS).with(httpBasic(correo, CLAVE)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[*].id", containsInAnyOrder(catalogo.keySet().stream().map(UUID::toString).toArray())))
                    .andReturn().getResponse().getContentAsString();
            if (primera == null) primera = respuesta;
            else assertEquals(primera, respuesta);
        }
        assertEquals(inicial, catalogo);
        // El orden según la intercalación MySQL pertenece al adaptador JDBC, no a este doble.
    }

    @Test
    @DisplayName("CP-AC-007: administrador no crea ni consulta espacios; anónimo recibe 401")
    void restringeCatalogoPorRolYAutenticacion() throws Exception {
        UUID id = UUID.randomUUID();
        catalogo.put(id, new EspacioAcademico(id, "Existente", "2026-2"));
        var inicial = Map.copyOf(catalogo);
        mvc.perform(get(ESPACIOS).with(httpBasic(ADMINISTRADOR, CLAVE))).andExpect(status().isForbidden());
        mvc.perform(get(ESPACIOS + "/{id}", id).with(httpBasic(ADMINISTRADOR, CLAVE)))
                .andExpect(status().isForbidden());
        modificar(post(ESPACIOS), ADMINISTRADOR, datosEspacio("No autorizado", "2026-2"))
                .andExpect(status().isForbidden());
        mvc.perform(get(ESPACIOS)).andExpect(status().isUnauthorized());
        assertEquals(inicial, catalogo);
        verifyNoInteractions(espacios);
    }

    private Usuario agregarCuenta(String correo, Rol rol, String resumen) {
        Usuario usuario = new Usuario(UUID.randomUUID(), correo, "Cuenta de prueba", rol, resumen, true, Instant.now());
        cuentas.put(usuario.id(), usuario);
        return usuario;
    }

    private Usuario cuenta(String correo) {
        return cuentas.values().stream().filter(usuario -> usuario.correo().equals(correo)).findFirst().orElseThrow();
    }

    private ResultActions consultarIdentidad(String correo) throws Exception {
        return mvc.perform(get("/api/identidad/yo").with(httpBasic(correo, CLAVE)));
    }

    /** Usa el endpoint CSRF real y conserva su cookie; no inyecta un token de prueba. */
    private ResultActions modificar(MockHttpServletRequestBuilder solicitud, String correo, String datos) throws Exception {
        var respuesta = mvc.perform(get("/api/identidad/csrf").with(httpBasic(correo, CLAVE)))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie cookie = respuesta.getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        String cabecera = JsonPath.read(respuesta.getContentAsString(), "$.nombreCabecera");
        String token = JsonPath.read(respuesta.getContentAsString(), "$.token");
        return mvc.perform(solicitud.with(httpBasic(correo, CLAVE)).cookie(cookie).header(cabecera, token)
                .contentType(MediaType.APPLICATION_JSON).content(datos));
    }

    private String datosDocente(String correo, String nombre, String clave) {
        return "{\"correo\":\"" + correo + "\",\"nombre\":\"" + nombre + "\",\"clave\":\"" + clave + "\"}";
    }

    private String datosEspacio(String nombre, String periodo) {
        return "{\"nombre\":\"" + nombre + "\",\"periodo\":\"" + periodo + "\"}";
    }
}
