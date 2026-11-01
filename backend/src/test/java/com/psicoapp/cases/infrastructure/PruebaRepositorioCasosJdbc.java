package com.psicoapp.cases.infrastructure;

import com.psicoapp.cases.domain.*;
import com.psicoapp.cases.DatosPruebaCasos;
import com.psicoapp.cases.application.ServicioPublicacionCaso;
import com.psicoapp.cases.application.PublicacionInvalida;
import com.psicoapp.identity.application.DirectorioIdentidad;
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

    @Test
    void editaYArchivaSinHuerfanos() {
        var original = DatosPruebaCasos.completo(docente);
        transaccion.executeWithoutResult(estado -> repositorio.guardar(original));
        var nuevo = DatosPruebaCasos.completo(docente);
        var actualizado = new Caso(original.id(), docente, "Editado", nuevo.desarrolloSituacional(), nuevo.contexto(),
                30, nuevo.escenarios(), nuevo.rubrica());
        transaccion.executeWithoutResult(estado -> {
            repositorio.bloquearPorId(original.id()).orElseThrow();
            repositorio.actualizarBorrador(actualizado);
        });
        assertEquals(actualizado, repositorio.buscarPorId(original.id()).orElseThrow());
        assertEquals(0, jdbc.sql("SELECT COUNT(*) FROM pregunta WHERE id_pregunta=?")
                .param(original.escenarios().getFirst().preguntas().getFirst().id().toString()).query(Integer.class).single());
        transaccion.executeWithoutResult(estado -> {
            repositorio.bloquearPorId(original.id()).orElseThrow();
            assertTrue(repositorio.archivar(original.id(), docente));
        });
        assertEquals("ARCHIVADO", repositorio.buscarPorId(original.id()).orElseThrow().estado());
        assertEquals(actualizado.escenarios(), repositorio.buscarPorId(original.id()).orElseThrow().escenarios());
        assertThrows(com.psicoapp.shared.exceptions.Conflicto.class,
                () -> transaccion.executeWithoutResult(estado -> repositorio.actualizarBorrador(actualizado)));
    }

    @Test
    void revierteEdicionCompletaAnteConflicto() {
        var original = DatosPruebaCasos.completo(docente);
        var ajeno = DatosPruebaCasos.completo(docente);
        transaccion.executeWithoutResult(estado -> { repositorio.guardar(original); repositorio.guardar(ajeno); });
        var invalido = new Caso(original.id(), docente, "Cambio", "Desarrollo", "Contexto", 30, ajeno.escenarios(), ajeno.rubrica());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> transaccion.executeWithoutResult(estado -> {
            repositorio.bloquearPorId(original.id());
            repositorio.actualizarBorrador(invalido);
        }));
        assertEquals(original, repositorio.buscarPorId(original.id()).orElseThrow());
    }
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
    @DisplayName("RF-09: publica una sola vez, conserva contenido y exige propietario")
    void actualizaSoloEstadoDelBorradorResponsable() {
        var caso = DatosPruebaCasos.completo(docente);
        transaccion.executeWithoutResult(estado -> repositorio.guardar(caso));
        assertFalse(repositorio.publicar(caso.id(), UUID.randomUUID()));
        assertFalse(repositorio.publicar(UUID.randomUUID(), docente));
        assertTrue(repositorio.publicar(caso.id(), docente));
        assertFalse(repositorio.publicar(caso.id(), docente));
        var publicado = repositorio.buscarPorId(caso.id()).orElseThrow();
        assertEquals("PUBLICADO", publicado.estado());
        assertEquals(caso.escenarios(), publicado.escenarios());
        assertEquals(caso.rubrica(), publicado.rubrica());
        assertThrows(IllegalArgumentException.class, () -> repositorio.guardar(publicado));
    }

    @Test
    @DisplayName("RF-09 / RN-03: publicacion real conserva invalido y confirma valido sin duplicar contenido")
    void integraValidacionYTransaccion() {
        var identidades = org.mockito.Mockito.mock(DirectorioIdentidad.class);
        var servicio = new ServicioPublicacionCaso(identidades, repositorio);
        var invalido = new ConstructorCasoAcademico(UUID.randomUUID(), docente).construir();
        var valido = DatosPruebaCasos.completo(docente);
        transaccion.executeWithoutResult(estado -> { repositorio.guardar(invalido); repositorio.guardar(valido); });
        assertThrows(PublicacionInvalida.class, () -> transaccion.executeWithoutResult(
                estado -> servicio.publicar(docente, invalido.id())));
        assertEquals("BORRADOR", repositorio.buscarPorId(invalido.id()).orElseThrow().estado());
        transaccion.executeWithoutResult(estado -> servicio.publicar(docente, valido.id()));
        assertEquals("PUBLICADO", repositorio.buscarPorId(valido.id()).orElseThrow().estado());
        assertEquals(1, jdbc.sql("SELECT COUNT(*) FROM escenario WHERE id_caso = ?")
                .param(valido.id().toString()).query(Integer.class).single());
    }

    @Test
    @DisplayName("RF-09: un fallo transaccional posterior revierte el cambio de estado")
    void reviertePublicacionSiFallaTransaccion() {
        var caso = DatosPruebaCasos.completo(docente);
        transaccion.executeWithoutResult(estado -> repositorio.guardar(caso));
        assertThrows(IllegalStateException.class, () -> transaccion.executeWithoutResult(estado -> {
            assertTrue(repositorio.publicar(caso.id(), docente));
            throw new IllegalStateException("Fallo posterior");
        }));
        assertEquals("BORRADOR", repositorio.buscarPorId(caso.id()).orElseThrow().estado());
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
