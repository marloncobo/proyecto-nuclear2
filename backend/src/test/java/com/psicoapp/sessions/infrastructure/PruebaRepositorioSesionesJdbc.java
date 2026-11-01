package com.psicoapp.sessions.infrastructure;

import com.psicoapp.sessions.domain.*;
import com.psicoapp.cases.DatosPruebaCasos;
import com.psicoapp.cases.domain.ConstructorCasoAcademico;
import com.psicoapp.cases.infrastructure.RepositorioCasosJdbc;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import static com.psicoapp.sessions.DatosPruebaSesiones.*;
import static org.junit.jupiter.api.Assertions.*;

class PruebaRepositorioSesionesJdbc {

    @Test
    void falloDeConvocatoriaRevierteSesionYAltasPrevias() {
        var jdbc = JdbcClient.create(fuente);
        UUID estudiante = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO usuario (id_usuario,nombre,correo,activo,fecha_creacion,id_rol)
                VALUES (?,'Alumno','alumno@example.test',true,CURRENT_TIMESTAMP,
                    '00000000-0000-0000-0000-000000000003')
                """).param(estudiante.toString()).update();
        var convocatorias = new RepositorioConvocatoriasJdbc(jdbc);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> transaccion.executeWithoutResult(estado -> {
            repositorio.guardar(sesion);
            convocatorias.guardar(new Convocatoria(UUID.randomUUID(),sesion.id(),estudiante,Convocatoria.resumir("uno"),true));
            convocatorias.guardar(new Convocatoria(UUID.randomUUID(),sesion.id(),UUID.randomUUID(),Convocatoria.resumir("dos"),true));
        }));
        assertTrue(repositorio.buscarPorId(sesion.id()).isEmpty());
        assertTrue(convocatorias.listarPorSesion(sesion.id()).isEmpty());
    }

    @Test
    void publicacionEsperaEdicionYValidaElContenidoConfirmado() throws Exception {
        var casos = new RepositorioCasosJdbc(JdbcClient.create(fuente));
        var caso = DatosPruebaCasos.completo(sesion.idDocente());
        transaccion.executeWithoutResult(estado -> casos.guardar(caso));
        var edicionEnCurso = new CountDownLatch(1);
        var liberarEdicion = new CountDownLatch(1);
        var publicacionSolicitada = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var edicion = ejecutor.submit(() -> transaccion.executeWithoutResult(estado -> {
                casos.bloquearPorId(caso.id()).orElseThrow();
                casos.actualizarBorrador(new ConstructorCasoAcademico(caso.id(), caso.idDocente()).construir());
                edicionEnCurso.countDown();
                try {
                    if (!liberarEdicion.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("No se libero edicion");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
            }));
            assertTrue(edicionEnCurso.await(5,TimeUnit.SECONDS));
            var publicacion = ejecutor.submit(() -> {
                var servicio = new com.psicoapp.cases.application.ServicioPublicacionCaso(
                        org.mockito.Mockito.mock(com.psicoapp.identity.application.DirectorioIdentidad.class),casos);
                assertThrows(com.psicoapp.cases.application.PublicacionInvalida.class, () ->
                    transaccion.executeWithoutResult(estado -> {
                        publicacionSolicitada.countDown();
                        servicio.publicar(caso.idDocente(),caso.id());
                    }));
            });
            assertTrue(publicacionSolicitada.await(5,TimeUnit.SECONDS));
            try {
                assertThrows(TimeoutException.class, () -> publicacion.get(200,TimeUnit.MILLISECONDS));
            } finally {
                liberarEdicion.countDown();
            }
            edicion.get(5,TimeUnit.SECONDS);
            publicacion.get(5,TimeUnit.SECONDS);
            assertEquals("BORRADOR",casos.buscarPorId(caso.id()).orElseThrow().estado());
        } finally {
            liberarEdicion.countDown();
        }
    }
    private HikariDataSource fuente;
    private RepositorioSesionesJdbc repositorio;
    private TransactionTemplate transaccion;
    private Sesion sesion;

    @BeforeEach
    void preparar() {
        fuente = new HikariDataSource();
        fuente.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        fuente.setUsername("sa");
        fuente.setPassword("");
        fuente.setMaximumPoolSize(2);
        fuente.setMinimumIdle(2);
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(fuente);
        var jdbc = JdbcClient.create(fuente);
        repositorio = new RepositorioSesionesJdbc(jdbc);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        UUID docente = UUID.randomUUID();
        UUID espacio = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO usuario (id_usuario,nombre,correo,activo,fecha_creacion,id_rol)
                VALUES (?, 'Docente', 'docente@example.test', true, CURRENT_TIMESTAMP,
                '00000000-0000-0000-0000-000000000002')
                """).param(docente.toString()).update();
        jdbc.sql("INSERT INTO espacio_academico VALUES (?, 'Psicologia', '2026-2')").param(espacio.toString()).update();
        var caso = DatosPruebaCasos.completo(docente);
        var casos = new RepositorioCasosJdbc(jdbc);
        transaccion.executeWithoutResult(estado -> { casos.guardar(caso); casos.publicar(caso.id(), docente); });
        sesion = Sesion.programar(UUID.randomUUID(), caso.id(), espacio, docente,
                INICIO, FIN, 30, 40, ModoNavegacion.FLEXIBLE, null, "Mensaje");
    }

    @AfterEach
    void cerrar() {
        fuente.close();
    }

    @Test
    @DisplayName("RF-18/19: persiste programacion, campos opcionales y reconstruye cada objeto State")
    void recuperaProgramacionYEstados() {
        repositorio.guardar(sesion);
        assertEquals(sesion, repositorio.buscarPorId(sesion.id()).orElseThrow());
        transaccion.executeWithoutResult(estado -> {
            var bloqueada = repositorio.bloquearPorId(sesion.id()).orElseThrow();
            repositorio.actualizarEstado(bloqueada.iniciar(INICIO));
        });
        assertSame(EnCurso.INSTANCIA, repositorio.buscarPorId(sesion.id()).orElseThrow().estado());
        transaccion.executeWithoutResult(estado -> {
            var bloqueada = repositorio.bloquearPorId(sesion.id()).orElseThrow();
            repositorio.actualizarEstado(bloqueada.finalizar(MotivoCierre.DOCENTE, FIN));
        });
        assertSame(Finalizado.INSTANCIA, repositorio.buscarPorId(sesion.id()).orElseThrow().estado());
        assertEquals(sesion.inicioPrevisto(), repositorio.buscarPorId(sesion.id()).orElseThrow().inicioPrevisto());
        assertTrue(repositorio.buscarPorId(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("RN-12: consulta vencidas incluye limite y excluye finalizadas")
    void seleccionaSoloAbiertasAlVencer() {
        repositorio.guardar(sesion);
        assertTrue(repositorio.buscarVencidas(FIN.minusSeconds(1)).isEmpty());
        assertEquals(List.of(sesion.id()), repositorio.buscarVencidas(FIN));
        transaccion.executeWithoutResult(estado -> {
            var bloqueada = repositorio.bloquearPorId(sesion.id()).orElseThrow();
            repositorio.actualizarEstado(bloqueada.finalizar(MotivoCierre.AUTOMATICO, FIN.plus(Duration.ofDays(1))));
        });
        assertTrue(repositorio.buscarVencidas(FIN).isEmpty());
    }

    @Test
    @DisplayName("RF-19: error transaccional revierte el estado sin perder la programacion")
    void revierteEstadoAnteFallo() {
        repositorio.guardar(sesion);
        assertThrows(IllegalStateException.class, () -> transaccion.executeWithoutResult(estado -> {
            repositorio.actualizarEstado(repositorio.bloquearPorId(sesion.id()).orElseThrow().iniciar(INICIO));
            throw new IllegalStateException("Fallo posterior");
        }));
        assertEquals(sesion, repositorio.buscarPorId(sesion.id()).orElseThrow());
    }

    @Test
    @DisplayName("RN-12 / RF-25: bloqueo serializa cierre y consulta de participacion concurrente")
    void bloqueoImpideLeerEnCursoDuranteCierreNoConfirmado() throws Exception {
        repositorio.guardar(sesion);
        transaccion.executeWithoutResult(estado -> repositorio.actualizarEstado(sesion.iniciar(INICIO)));
        var cierreBloqueado = new CountDownLatch(1);
        var liberarCierre = new CountDownLatch(1);
        var lecturaSolicitada = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var cierre = ejecutor.submit(() -> transaccion.executeWithoutResult(estado -> {
                var bloqueada = repositorio.bloquearPorId(sesion.id()).orElseThrow();
                repositorio.actualizarEstado(bloqueada.finalizar(MotivoCierre.DOCENTE, FIN));
                cierreBloqueado.countDown();
                try {
                    if (!liberarCierre.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("No se libero el cierre");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
            }));
            assertTrue(cierreBloqueado.await(5, TimeUnit.SECONDS));
            var lectura = ejecutor.submit(() -> transaccion.execute(estado -> {
                lecturaSolicitada.countDown();
                var bloqueada = repositorio.bloquearPorId(sesion.id()).orElseThrow();
                assertThrows(TransicionSesionInvalida.class, () -> bloqueada.validarParticipacion(FIN));
                return bloqueada.estado().codigo();
            }));
            assertTrue(lecturaSolicitada.await(5, TimeUnit.SECONDS));
            try {
                assertThrows(TimeoutException.class, () -> lectura.get(200, TimeUnit.MILLISECONDS));
            } finally {
                liberarCierre.countDown();
            }
            cierre.get(5, TimeUnit.SECONDS);
            assertEquals(TipoEstadoSesion.FINALIZADO, lectura.get(5, TimeUnit.SECONDS));
        } finally {
            liberarCierre.countDown();
        }
    }
}
