package com.psicoapp.participation.infrastructure;

import com.psicoapp.participation.domain.EstadoIntento;
import com.psicoapp.participation.domain.Intento;
import com.psicoapp.sessions.domain.ModoNavegacion;
import com.zaxxer.hikari.HikariDataSource;
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

class PruebaRepositorioIntentosJdbc {
    private static final Instant INICIO = Instant.parse("2026-01-01T10:00:00Z");
    private HikariDataSource fuente;
    private TransactionTemplate transaccion;
    private RepositorioIntentosJdbc repositorio;
    private UUID estudiante;
    private UUID sesion;
    private UUID pregunta;
    private UUID opcion;
    private UUID caso;

    @BeforeEach
    void preparar() {
        fuente = new HikariDataSource();
        fuente.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        fuente.setUsername("sa");
        fuente.setPassword("");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(fuente);
        JdbcClient jdbc = JdbcClient.create(fuente);
        repositorio = new RepositorioIntentosJdbc(jdbc);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));

        UUID docente = UUID.randomUUID();
        UUID espacio = UUID.randomUUID();
        caso = UUID.randomUUID();
        UUID escenario = UUID.randomUUID();
        pregunta = UUID.randomUUID();
        opcion = UUID.randomUUID();
        estudiante = UUID.randomUUID();
        sesion = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO usuario (id_usuario,nombre,correo,activo,fecha_creacion,id_rol)
                VALUES (?, 'Docente', ?, true, CURRENT_TIMESTAMP, ?),
                       (?, 'Estudiante', ?, true, CURRENT_TIMESTAMP, ?)
                """).params(docente.toString(), docente + "@example.test",
                "00000000-0000-0000-0000-000000000002", estudiante.toString(),
                estudiante + "@example.test", "00000000-0000-0000-0000-000000000003").update();
        jdbc.sql("INSERT INTO espacio_academico VALUES (?, 'Psicologia', '2026-2')")
                .param(espacio.toString()).update();
        jdbc.sql("""
                INSERT INTO caso (id_caso,id_usuario_docente,nombre,desarrollo_situacional,contexto,
                    tiempo_estimado_min,estado)
                VALUES (?,?,'Caso','Desarrollo','Contexto',30,'PUBLICADO')
                """).params(caso.toString(), docente.toString()).update();
        jdbc.sql("INSERT INTO escenario VALUES (?,?,'Escenario','Contexto',1)")
                .params(escenario.toString(), caso.toString()).update();
        jdbc.sql("INSERT INTO pregunta VALUES (?,?,'Pregunta',1)")
                .params(pregunta.toString(), escenario.toString()).update();
        jdbc.sql("INSERT INTO opcion_respuesta VALUES (?,?,'Opcion',true,'Justificacion')")
                .params(opcion.toString(), pregunta.toString()).update();
        jdbc.sql("""
                INSERT INTO sesion VALUES (?, ?, ?, ?, ?, ?, 30, 'FLEXIBLE', 'EN_CURSO', NULL, NULL)
                """).params(sesion.toString(), caso.toString(), espacio.toString(), docente.toString(),
                java.sql.Timestamp.from(INICIO), java.sql.Timestamp.from(INICIO.plusSeconds(3600))).update();
    }

    @AfterEach
    void cerrar() {
        fuente.close();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("RF-15 / RN-06: filtra estudiante, caso y estado conservando el historial")
    void consultaSoloIntentosTerminadosDelEstudianteYCaso() {
        Intento base = Intento.iniciar(UUID.randomUUID(), sesion, estudiante, INICIO, 30,
                ModoNavegacion.FLEXIBLE, List.of(pregunta)).responder(pregunta, opcion, INICIO.plusSeconds(1));
        Intento finalizado = base.finalizar(INICIO.plusSeconds(2));
        Intento vencido = Intento.iniciar(UUID.randomUUID(), sesion, estudiante, INICIO.plusSeconds(10), 30,
                ModoNavegacion.FLEXIBLE, List.of(pregunta)).vencer(INICIO.plusSeconds(1810));
        Intento activo = Intento.iniciar(UUID.randomUUID(), sesion, estudiante, INICIO.plusSeconds(20), 30,
                ModoNavegacion.FLEXIBLE, List.of(pregunta));
        transaccion.executeWithoutResult(estado -> {
            repositorio.guardar(finalizado);
            repositorio.guardar(vencido);
            repositorio.guardar(activo);
        });
        var historial = repositorio.buscarTerminadosPorEstudianteYCaso(estudiante, caso);
        assertEquals(List.of(finalizado.id(), vencido.id()), historial.stream().map(Intento::id).toList());
        assertEquals(opcion, historial.getFirst().respuestas().get(pregunta));
        assertTrue(historial.get(1).respuestas().isEmpty());
        assertTrue(repositorio.buscarTerminadosPorEstudianteYCaso(UUID.randomUUID(), caso).isEmpty());
        assertTrue(repositorio.buscarTerminadosPorEstudianteYCaso(estudiante, UUID.randomUUID()).isEmpty());
    }

    @Test
    void persisteYReconstruyeIntentoConRespuesta() {
        Intento intento = Intento.iniciar(UUID.randomUUID(), sesion, estudiante, INICIO, 30,
                ModoNavegacion.FLEXIBLE, List.of(pregunta)).responder(pregunta, opcion, INICIO.plusSeconds(1));

        transaccion.executeWithoutResult(estado -> repositorio.guardar(intento));
        Intento recuperado = repositorio.buscarPorId(intento.id()).orElseThrow();

        assertEquals(intento.id(), recuperado.id());
        assertEquals(List.of(pregunta), recuperado.preguntas());
        assertEquals(opcion, recuperado.respuestas().get(pregunta));
        assertEquals(EstadoIntento.EN_PROGRESO, recuperado.estado());
    }

    @Test
    void actualizarRespuestaYEstadoMantieneLaFilaUnica() {
        Intento intento = Intento.iniciar(UUID.randomUUID(), sesion, estudiante, INICIO, 30,
                ModoNavegacion.FLEXIBLE, List.of(pregunta));
        transaccion.executeWithoutResult(estado -> repositorio.guardar(intento));
        Intento finalizado = intento.responder(pregunta, opcion, INICIO.plusSeconds(1))
                .finalizar(INICIO.plusSeconds(2));

        transaccion.executeWithoutResult(estado -> repositorio.actualizar(finalizado));
        Intento recuperado = repositorio.buscarPorId(intento.id()).orElseThrow();

        assertEquals(EstadoIntento.FINALIZADO, recuperado.estado());
        assertEquals(opcion, recuperado.respuestas().get(pregunta));
        assertEquals(1, JdbcClient.create(fuente).sql("SELECT COUNT(*) FROM respuesta WHERE id_intento=?")
                .param(intento.id().toString()).query(Integer.class).single());
    }
}
