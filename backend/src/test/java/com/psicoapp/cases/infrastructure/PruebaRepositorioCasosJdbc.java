package com.psicoapp.cases.infrastructure;

import com.psicoapp.cases.domain.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

class PruebaRepositorioCasosJdbc {
    private RepositorioCasosJdbc repositorio;
    private JdbcClient jdbc;
    private TransactionTemplate transaccion;
    private SingleConnectionDataSource fuente;
    private final UUID docente = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        fuente = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "", true);
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(fuente);
        jdbc = JdbcClient.create(fuente);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        repositorio = new RepositorioCasosJdbc(jdbc);
        jdbc.sql("""
                INSERT INTO usuario (id_usuario,nombre,correo,activo,fecha_creacion,id_rol)
                VALUES (?, 'Docente', 'docente@example.test', true, CURRENT_TIMESTAMP,
                    '00000000-0000-0000-0000-000000000002')
                """).param(docente.toString()).update();
    }

    @AfterEach
    void cerrar() {
        fuente.destroy();
    }

    @Test
    @DisplayName("RF-05/06/07/08/17: persiste y recupera agregado y relaciones de rubrica")
    void recuperaCasoCompleto() {
        var opcion = new OpcionRespuesta(UUID.randomUUID(), "Escuchar", true, "Reconoce perspectivas");
        var pregunta = new Pregunta(UUID.randomUUID(), "Como intervenir", 1, List.of(opcion));
        var escenario = new Escenario(UUID.randomUUID(), "Reunion", "Desacuerdo", 1, List.of(pregunta));
        var criterio = new CriterioRubrica(UUID.randomUUID(), "Analisis", new BigDecimal("1.00"), List.of(pregunta.id()));
        var rubrica = new Rubrica(UUID.randomUUID(), "Rubrica", new BigDecimal("5.00"), List.of(criterio));
        var caso = new ConstructorCasoAcademico(UUID.randomUUID(), docente).definirDatos("Caso", "Desarrollo", "Historia", 40)
                .agregarEscenario(escenario).asociarRubrica(rubrica).construir();
        transaccion.executeWithoutResult(estado -> repositorio.guardar(caso));
        assertEquals(caso, repositorio.buscarPorId(caso.id()).orElseThrow());
    }

    @Test
    @DisplayName("RF-09: guarda borrador incompleto sin convertir tiempo omitido a cero")
    void recuperaBorradorIncompleto() {
        var caso = new ConstructorCasoAcademico(UUID.randomUUID(), docente).construir();
        transaccion.executeWithoutResult(estado -> repositorio.guardar(caso));
        assertEquals(caso, repositorio.buscarPorId(caso.id()).orElseThrow());
        assertTrue(repositorio.buscarPorId(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("RF-05: fallo al insertar un hijo revierte el agregado completo")
    void revierteAnteConflictoEnContenido() {
        UUID idEscenario = UUID.randomUUID();
        var primero = new ConstructorCasoAcademico(UUID.randomUUID(), docente)
                .agregarEscenario(new Escenario(idEscenario, "", "", 1, List.of())).construir();
        transaccion.executeWithoutResult(estado -> repositorio.guardar(primero));
        var segundo = new ConstructorCasoAcademico(UUID.randomUUID(), docente)
                .agregarEscenario(new Escenario(idEscenario, "", "", 1, List.of())).construir();
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> transaccion.executeWithoutResult(estado -> repositorio.guardar(segundo)));
        assertTrue(repositorio.buscarPorId(segundo.id()).isEmpty());
        assertTrue(repositorio.buscarPorId(primero.id()).isPresent());
    }
}
