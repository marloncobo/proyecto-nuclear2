package com.psicoapp.notifications;

import com.psicoapp.cases.DatosPruebaCasos;
import com.psicoapp.cases.application.ServicioCreacionCaso.DatosCreacionCaso;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;

/** Reutiliza el recorrido completo exclusivamente en un esquema temporal propio. */
@EnabledIfEnvironmentVariable(named = "PSICOAPP_PRUEBAS_MYSQL_URL", matches = "jdbc:mysql://.+")
class PruebaFlujosNotificacionesMySql extends PruebaFlujosNotificaciones {
    private static final String ESQUEMA = "psicoapp_notificaciones_prueba_" + UUID.randomUUID().toString().replace("-", "");
    private static String servidor;
    private static boolean creado;

    @BeforeAll
    static void aplicarMigracion(@Autowired DataSource fuente) throws Exception {
        String sql = Files.readString(Path.of("migraciones", "006-integridad-academica.sql"))
                .replaceAll("(?m)^DELIMITER .*\\R?", "");
        var recurso = new EncodedResource(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
        try (var conexion = fuente.getConnection()) {
            ScriptUtils.executeSqlScript(conexion, recurso, false, false,
                    ScriptUtils.DEFAULT_COMMENT_PREFIX, "$$", "/*", "*/");
        }
    }

    @Test
    void bloqueaEscriturasInvalidasPorSqlDirecto() {
        String borrador = UUID.randomUUID().toString();
        jdbc.sql("""
                INSERT INTO caso (id_caso,id_usuario_docente,nombre,desarrollo_situacional,contexto,estado)
                VALUES (?,?,'Caso','Desarrollo','Contexto','BORRADOR')
                """).params(borrador, docente.toString()).update();
        assertSignal(() -> jdbc.sql("UPDATE caso SET estado='PUBLICADO' WHERE id_caso=?").param(borrador).update());
        assertSignal(() -> jdbc.sql("UPDATE sesion SET duracion_maxima_min=41 WHERE id_sesion=?")
                .param(sesion.toString()).update());
        assertSignal(() -> jdbc.sql("""
                INSERT INTO sesion (id_sesion,id_caso,id_espacio,id_docente,fecha_inicio,fecha_fin,
                    duracion_maxima_min,modo_navegacion,estado)
                SELECT UUID(),id_caso,id_espacio,id_docente,fecha_inicio,DATE_ADD(fecha_inicio, INTERVAL 1 MINUTE),
                    duracion_maxima_min,modo_navegacion,'SIN_INICIAR'
                FROM sesion WHERE id_sesion=?
                """).param(sesion.toString()).update());

        sesiones.iniciar(docente, sesion);
        var intento = participacion.iniciar(estudiante, sesion);
        String id = intento.id().toString();
        var otraSesion = sesiones.programar(docente, programacion);
        sesiones.iniciar(docente, otraSesion.id());
        assertThrows(DuplicateKeyException.class, () -> jdbc.sql("""
                INSERT INTO intento (id_intento,id_sesion,id_estudiante,fecha_inicio,fecha_limite,
                    posicion_actual,modo_navegacion,estado,fecha_fin)
                SELECT UUID(),?,id_estudiante,fecha_inicio,fecha_limite,0,modo_navegacion,'EN_PROGRESO',NULL
                FROM intento WHERE id_intento=?
                """).params(otraSesion.id().toString(), id).update());
        assertSignal(() -> jdbc.sql("INSERT INTO evaluacion VALUES (?,0,CURRENT_TIMESTAMP)")
                .param(id).update());

        var datos = DatosPruebaCasos.completo(docente);
        var preguntaAjena = datos.escenarios().getFirst().preguntas().getFirst();
        var opcionAjena = preguntaAjena.opciones().getFirst().id();
        var criterioAjeno = datos.rubrica().criterios().getFirst().id();
        creacion.crear(docente, new DatosCreacionCaso(
                datos.nombre(), datos.desarrolloSituacional(), datos.contexto(), datos.tiempoEstimadoMin(),
                datos.escenarios(), datos.rubrica()));
        assertSignal(() -> jdbc.sql("INSERT INTO criterio_pregunta VALUES (?,?)")
                .params(criterioAjeno.toString(), pregunta.toString()).update());
        assertSignal(() -> jdbc.sql("INSERT INTO intento_pregunta VALUES (?,?,1)")
                .params(id, preguntaAjena.id().toString()).update());
        assertSignal(() -> jdbc.sql("INSERT INTO respuesta VALUES (?,?,?,CURRENT_TIMESTAMP)")
                .params(id, pregunta.toString(), opcionAjena.toString()).update());

        participacion.responder(estudiante, intento.id(), pregunta, opcion);
        participacion.finalizar(estudiante, intento.id());
        assertSignal(() -> jdbc.sql("DELETE FROM respuesta WHERE id_intento=?").param(id).update());
        assertSignal(() -> jdbc.sql("UPDATE intento SET estado='EN_PROGRESO',fecha_fin=NULL WHERE id_intento=?")
                .param(id).update());
        assertEquals(0, jdbc.sql("SELECT COUNT(*) FROM v_integridad_resultados WHERE id_intento=?")
                .param(id).query(Integer.class).single());
    }

    @Test
    void lasVistasDetectanIncidenciasPorRegistro() {
        assertEquals(0, jdbc.sql("SELECT COUNT(*) FROM v_integridad_casos WHERE id_caso=?")
                .param(caso.toString()).query(Integer.class).single());
        assertEquals(0, jdbc.sql("SELECT COUNT(*) FROM v_integridad_participacion WHERE id_sesion=?")
                .param(sesion.toString()).query(Integer.class).single());
        jdbc.sql("INSERT INTO ausencia_reporte VALUES (?,?,CURRENT_TIMESTAMP)")
                .params(sesion.toString(), estudiante.toString()).update();
        assertEquals("AUSENCIA_INCONSISTENTE",
                jdbc.sql("SELECT incidencia FROM v_integridad_resultados WHERE id_sesion=?")
                        .param(sesion.toString()).query(String.class).single());
    }

    private static void assertSignal(Executable escritura) {
        var error = assertThrows(DataAccessException.class, escritura);
        assertEquals("45000", ((SQLException) error.getMostSpecificCause()).getSQLState());
    }

    @DynamicPropertySource
    static void prepararBase(DynamicPropertyRegistry propiedades) throws Exception {
        servidor = System.getenv("PSICOAPP_PRUEBAS_MYSQL_URL");
        if (servidor == null || !servidor.matches("jdbc:mysql://[A-Za-z0-9.:-]+/"))
            throw new IllegalArgumentException("Configure una URL de servidor MySQL sin nombre de base");
        if (usuario() == null || clave() == null) throw new IllegalStateException("Configure credenciales de MySQL para pruebas");
        try (var conexion = DriverManager.getConnection(servidor, usuario(), clave());
                var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("CREATE DATABASE " + ESQUEMA + " CHARACTER SET utf8mb4");
            creado = true;
        }
        propiedades.add("spring.datasource.url", () -> servidor + ESQUEMA
                + "?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true");
        propiedades.add("spring.datasource.username", PruebaFlujosNotificacionesMySql::usuario);
        propiedades.add("spring.datasource.password", PruebaFlujosNotificacionesMySql::clave);
    }
    @AfterAll
    static void eliminarSoloEsquemaPropio() throws Exception {
        if (!creado || !ESQUEMA.matches("psicoapp_notificaciones_prueba_[a-f0-9]{32}")) return;
        try (var conexion = DriverManager.getConnection(servidor, usuario(), clave());
                var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("DROP DATABASE " + ESQUEMA);
        }
    }
    private static String usuario() { return System.getenv("PSICOAPP_PRUEBAS_MYSQL_USUARIO"); }
    private static String clave() { return System.getenv("PSICOAPP_PRUEBAS_MYSQL_CLAVE"); }
}
