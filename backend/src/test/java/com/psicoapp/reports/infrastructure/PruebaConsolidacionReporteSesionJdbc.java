package com.psicoapp.reports.infrastructure;

import com.psicoapp.sessions.application.ConsultaSesionesDocente;
import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CU-35: consolidacion durable de ausencias en reportes")
class PruebaConsolidacionReporteSesionJdbc {
    private HikariDataSource fuente;
    private JdbcClient jdbc;
    private ConsolidadorReporteSesionJdbc consolidador;
    private ConsultaSesionesDocente autorizacion;
    private UUID sesion;
    private UUID docente;
    private UUID ausente;
    private UUID participante;
    private UUID primeraPregunta;

    @BeforeEach
    void preparar() {
        fuente = new HikariDataSource();
        fuente.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        fuente.setUsername("sa");
        fuente.setPassword("");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(fuente);
        jdbc = JdbcClient.create(fuente);
        consolidador = new ConsolidadorReporteSesionJdbc(jdbc);
        autorizacion = mock(ConsultaSesionesDocente.class);

        docente = UUID.randomUUID();
        ausente = UUID.randomUUID();
        participante = UUID.randomUUID();
        UUID espacio = UUID.randomUUID();
        UUID caso = UUID.randomUUID();
        UUID escenario = UUID.randomUUID();
        primeraPregunta = UUID.randomUUID();
        UUID segundaPregunta = UUID.randomUUID();
        sesion = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO usuario (id_usuario,nombre,correo,activo,fecha_creacion,id_rol)
                VALUES (?, 'Docente', ?, true, CURRENT_TIMESTAMP, ?),
                       (?, 'Ana Ausente', 'ana@ejemplo.edu', true, CURRENT_TIMESTAMP, ?),
                       (?, 'Beto Participante', 'beto@ejemplo.edu', true, CURRENT_TIMESTAMP, ?)
                """).params(docente.toString(), docente + "@ejemplo.edu",
                "00000000-0000-0000-0000-000000000002", ausente.toString(),
                "00000000-0000-0000-0000-000000000003", participante.toString(),
                "00000000-0000-0000-0000-000000000003").update();
        jdbc.sql("INSERT INTO espacio_academico VALUES (?, 'Psicologia', '2026-2')")
                .param(espacio.toString()).update();
        jdbc.sql("""
                INSERT INTO caso (id_caso,id_usuario_docente,nombre,desarrollo_situacional,contexto,
                    tiempo_estimado_min,estado) VALUES (?,?,'Caso','Desarrollo','Contexto',30,'PUBLICADO')
                """).params(caso.toString(), docente.toString()).update();
        jdbc.sql("INSERT INTO escenario VALUES (?,?,'Escenario','Contexto',1)")
                .params(escenario.toString(), caso.toString()).update();
        jdbc.sql("INSERT INTO pregunta VALUES (?,?,'Primera',1),(?,?,'Segunda',2)")
                .params(primeraPregunta.toString(), escenario.toString(), segundaPregunta.toString(),
                        escenario.toString()).update();
        jdbc.sql("""
                INSERT INTO sesion VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP,
                    DATEADD('HOUR', 1, CURRENT_TIMESTAMP), 30, 'FLEXIBLE', 'FINALIZADO', NULL, NULL)
                """).params(sesion.toString(), caso.toString(), espacio.toString(), docente.toString()).update();
        jdbc.sql("""
                INSERT INTO convocatoria VALUES (?, ?, ?, 'codigo-a', 'AUTORIZADA'),
                                                (?, ?, ?, 'codigo-b', 'AUTORIZADA')
                """).params(UUID.randomUUID().toString(), sesion.toString(), ausente.toString(),
                        UUID.randomUUID().toString(), sesion.toString(), participante.toString()).update();
        UUID intento = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO intento (id_intento,id_sesion,id_estudiante,fecha_inicio,fecha_limite,
                    posicion_actual,modo_navegacion,estado) VALUES (?, ?, ?, CURRENT_TIMESTAMP,
                    DATEADD('MINUTE', 30, CURRENT_TIMESTAMP), 0, 'FLEXIBLE', 'EN_PROGRESO')
                """).params(intento.toString(), sesion.toString(), participante.toString()).update();
        jdbc.sql("INSERT INTO intento_pregunta VALUES (?, ?, 0), (?, ?, 1)")
                .params(intento.toString(), primeraPregunta.toString(), intento.toString(), segundaPregunta.toString()).update();
        UUID opcion = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO opcion_respuesta (id_opcion,id_pregunta,texto,es_correcta,justificacion)
                VALUES (?, ?, 'Seleccion', true, 'Explicacion')
                """).params(opcion.toString(), primeraPregunta.toString()).update();
        jdbc.sql("INSERT INTO respuesta VALUES (?, ?, ?, CURRENT_TIMESTAMP)")
                .params(intento.toString(), primeraPregunta.toString(), opcion.toString()).update();
    }

    @AfterEach
    void cerrar() {
        fuente.close();
    }

    @Test
    void registraSoloAlNoParticipanteConCeroPorPreguntaYEsIdempotente() {
        Instant cierre = Instant.parse("2026-11-21T15:00:00Z");

        consolidador.consolidar(sesion, cierre);
        consolidador.consolidar(sesion, cierre.plusSeconds(1));

        assertEquals(1, contar("ausencia_reporte"));
        assertEquals(2, contar("detalle_ausencia_reporte"));
        assertEquals(BigDecimal.ZERO.setScale(2), jdbc.sql("SELECT SUM(puntos) FROM detalle_ausencia_reporte")
                .query(BigDecimal.class).single());

        var filas = new ReporteSesionJdbc(jdbc, autorizacion).consultar(docente, sesion);
        assertEquals(2, filas.size());
        var filaAusente = filas.getFirst();
        assertEquals(ausente, filaAusente.idEstudiante());
        assertEquals("Ana Ausente", filaAusente.nombreEstudiante());
        assertEquals("AUSENTE", filaAusente.estado());
        assertEquals(0, BigDecimal.ZERO.compareTo(filaAusente.calificacion()));
        assertNotNull(filaAusente.ausencia());
        assertEquals(cierre, filaAusente.ausencia().fechaConsolidacion());
        assertEquals(2, filaAusente.ausencia().detalles().size());
        assertEquals(primeraPregunta, filaAusente.ausencia().detalles().getFirst().idPregunta());
        assertNull(filaAusente.preguntasRespondidas());
        assertNull(filaAusente.preguntasTotales());
        assertNull(filaAusente.inicioIntento());
        assertNull(filaAusente.preguntasCorrectas());
        assertEquals(1, filas.get(1).preguntasRespondidas());
        assertEquals(2, filas.get(1).preguntasTotales());
        assertEquals(1, filas.get(1).preguntasCorrectas());
        assertNotNull(filas.get(1).inicioIntento());
        assertNull(filas.get(1).finIntento());
        assertNull(filas.get(1).ausencia());
        assertEquals("EN_PROGRESO", filas.get(1).estado());
        verify(autorizacion).exigirResponsable(docente, sesion);
    }

    private int contar(String tabla) {
        return jdbc.sql("SELECT COUNT(*) FROM " + tabla).query(Integer.class).single();
    }
}
