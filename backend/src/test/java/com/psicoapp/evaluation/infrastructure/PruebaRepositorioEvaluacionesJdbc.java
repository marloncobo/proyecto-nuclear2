package com.psicoapp.evaluation.infrastructure;

import com.psicoapp.evaluation.application.ProveedorCasosEvaluables;
import com.psicoapp.evaluation.application.RepositorioEvaluaciones.EvaluacionPersistida;
import com.psicoapp.evaluation.domain.DetalleEvaluacion;
import com.psicoapp.evaluation.domain.ResultadoEvaluacion;
import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

class PruebaRepositorioEvaluacionesJdbc {
    private HikariDataSource fuente;
    private TransactionTemplate transaccion;
    private RepositorioEvaluacionesJdbc repositorio;
    private UUID intento;
    private UUID pregunta;
    private UUID sesion;

    @BeforeEach
    void preparar() {
        fuente = new HikariDataSource();
        fuente.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        fuente.setUsername("sa");
        fuente.setPassword("");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(fuente);
        JdbcClient jdbc = JdbcClient.create(fuente);
        repositorio = new RepositorioEvaluacionesJdbc(jdbc);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        intento = UUID.randomUUID();
        pregunta = UUID.randomUUID();
        UUID estudiante = UUID.randomUUID();
        UUID docente = UUID.randomUUID();
        UUID espacio = UUID.randomUUID();
        UUID caso = UUID.randomUUID();
        UUID escenario = UUID.randomUUID();
        UUID opcion = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO usuario (id_usuario,nombre,correo,activo,fecha_creacion,id_rol)
                VALUES (?, 'Docente', 'docente@example.test', true, CURRENT_TIMESTAMP, ?),
                       (?, 'Estudiante', 'estudiante@example.test', true, CURRENT_TIMESTAMP, ?)
                """).params(docente.toString(), "00000000-0000-0000-0000-000000000002",
                estudiante.toString(), "00000000-0000-0000-0000-000000000003").update();
        jdbc.sql("INSERT INTO espacio_academico VALUES (?, 'Psicologia', '2026-2')")
                .param(espacio.toString()).update();
        jdbc.sql("""
                INSERT INTO caso (id_caso,id_usuario_docente,nombre,desarrollo_situacional,contexto,
                    tiempo_estimado_min,estado) VALUES (?,?,'Caso','Desarrollo','Contexto',30,'PUBLICADO')
                """).params(caso.toString(), docente.toString()).update();
        jdbc.sql("INSERT INTO escenario VALUES (?,?,'Escenario','Contexto',1)")
                .params(escenario.toString(), caso.toString()).update();
        jdbc.sql("INSERT INTO pregunta VALUES (?,?,'Pregunta',1)")
                .params(pregunta.toString(), escenario.toString()).update();
        jdbc.sql("INSERT INTO opcion_respuesta VALUES (?,?,'Opcion',true,'Correcta')")
                .params(opcion.toString(), pregunta.toString()).update();
        UUID rubrica = UUID.randomUUID();
        UUID criterio = UUID.randomUUID();
        jdbc.sql("INSERT INTO rubrica VALUES (?,?,'Rubrica',5.00)")
                .params(rubrica.toString(), caso.toString()).update();
        jdbc.sql("INSERT INTO criterio_rubrica VALUES (?,?, 'Criterio', 1.00)")
                .params(criterio.toString(), rubrica.toString()).update();
        jdbc.sql("INSERT INTO criterio_pregunta VALUES (?,?)")
                .params(criterio.toString(), pregunta.toString()).update();
        sesion = UUID.randomUUID();
        jdbc.sql("INSERT INTO sesion VALUES (?, ?, ?, ?, ?, ?, 30, 'FLEXIBLE', 'EN_CURSO', NULL, NULL)")
                .params(sesion.toString(), caso.toString(), espacio.toString(), docente.toString(),
                java.sql.Timestamp.from(Instant.parse("2026-01-01T10:00:00Z")),
                java.sql.Timestamp.from(Instant.parse("2026-01-01T11:00:00Z"))).update();
        jdbc.sql("""
                INSERT INTO intento VALUES (?, ?, ?, ?, ?, 0, 'FLEXIBLE', 'FINALIZADO')
                """).params(intento.toString(), sesion.toString(), estudiante.toString(),
                java.sql.Timestamp.from(Instant.parse("2026-01-01T10:00:00Z")),
                java.sql.Timestamp.from(Instant.parse("2026-01-01T10:30:00Z"))).update();
        jdbc.sql("INSERT INTO intento_pregunta VALUES (?,?,0)")
                .params(intento.toString(), pregunta.toString()).update();
    }

    @AfterEach
    void cerrar() {
        fuente.close();
    }

    @Test
    void persisteYReconstruyeEvaluacionConDetalle() {
        DetalleEvaluacion detalle = new DetalleEvaluacion(pregunta, null, false, false,
                new BigDecimal("0.00"), "Pregunta sin responder");
        EvaluacionPersistida evaluacion = new EvaluacionPersistida(intento,
                new ResultadoEvaluacion(new BigDecimal("0.00"), List.of(detalle)),
                Instant.parse("2026-01-01T10:31:00Z"));

        transaccion.executeWithoutResult(estado -> repositorio.guardar(evaluacion));
        EvaluacionPersistida recuperada = repositorio.buscarPorIntento(intento).orElseThrow();

        assertEquals(new BigDecimal("0.00"), recuperada.resultado().calificacion());
        assertEquals(1, recuperada.resultado().detalles().size());
        assertFalse(recuperada.resultado().detalles().get(0).contestada());
    }

    @Test
    void obtieneContenidoPublicoDelCasoParaEvaluar() {
        var proveedor = new ProveedorCasosEvaluablesJdbc(JdbcClient.create(fuente));

        ProveedorCasosEvaluables.CasoEvaluable caso = proveedor.buscarPorSesion(sesion);

        assertEquals(1, caso.preguntas().size());
        assertEquals(pregunta, caso.preguntas().get(0).id());
        assertEquals(new BigDecimal("5.00"), caso.rubrica().escalaMaxima());
        assertEquals(1, caso.rubrica().criterios().size());
    }
}
