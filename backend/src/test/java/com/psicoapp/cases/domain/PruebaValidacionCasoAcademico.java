package com.psicoapp.cases.domain;

import com.psicoapp.cases.DatosPruebaCasos;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PruebaValidacionCasoAcademico {
    private final ValidacionCasoAcademico validacion = new ValidacionCasoAcademico();

    @Test
    @DisplayName("RF-09: Template Method mantiene orden y acumula errores de las tres etapas")
    void ejecutaEtapasEnOrdenSinCortarAlPrimerError() {
        List<String> etapas = new ArrayList<>();
        var plantilla = new PlantillaValidacionPublicacion() {
            protected void validarEstructura(Caso caso, List<ErrorValidacion> errores) {
                etapas.add("estructura");
                errores.add(new ErrorValidacion("ESTRUCTURA", "nombre", "Incompleto"));
            }
            protected void validarPreguntas(Caso caso, List<ErrorValidacion> errores) {
                etapas.add("preguntas");
                errores.add(new ErrorValidacion("PREGUNTAS", "escenarios", "Incompleto"));
            }
            protected void validarRubrica(Caso caso, List<ErrorValidacion> errores) {
                etapas.add("rubrica");
                errores.add(new ErrorValidacion("RUBRICA", "rubrica", "Incompleto"));
            }
        };
        var resultado = plantilla.validar(new ConstructorCasoAcademico(UUID.randomUUID(), UUID.randomUUID()).construir());
        assertEquals(List.of("estructura", "preguntas", "rubrica"), etapas);
        assertEquals(3, resultado.errores().size());
        assertFalse(resultado.esValido());
        assertThrows(UnsupportedOperationException.class, () -> resultado.errores().clear());
    }

    @Test
    @DisplayName("RF-09 / RN-03: caso completo con rubrica valida puede publicarse")
    void aceptaCasoCompletoSinMutarlo() {
        Caso caso = DatosPruebaCasos.completo(UUID.randomUUID());
        assertTrue(validacion.validar(caso).esValido());
        assertEquals("BORRADOR", caso.estado());
    }

    @Test
    @DisplayName("RF-05/09: reporta contenido academico, tiempo, escenarios y rubrica faltantes")
    void acumulaErroresDelBorradorVacio() {
        var caso = new ConstructorCasoAcademico(UUID.randomUUID(), UUID.randomUUID()).construir();
        var resultado = validacion.validar(caso);
        assertEquals(List.of("nombre", "desarrolloSituacional", "contexto", "tiempoEstimadoMin", "escenarios", "rubrica"),
                resultado.errores().stream().map(ErrorValidacion::ruta).toList());
    }

    @Test
    @DisplayName("RN-02/05/11: detecta huecos de orden, enunciados y justificaciones ausentes")
    void validaContenidoAnidadoYOrden() {
        var opcion = new OpcionRespuesta(UUID.randomUUID(), "", true, "");
        var pregunta = new Pregunta(UUID.randomUUID(), "", 2, List.of(opcion));
        var caso = new ConstructorCasoAcademico(UUID.randomUUID(), UUID.randomUUID())
                .agregarEscenario(new Escenario(UUID.randomUUID(), "", "", 3, List.of(pregunta))).construir();
        var errores = validacion.validar(caso).errores();
        assertEquals(2, errores.stream().filter(e -> e.codigo().equals("ORDEN_NO_CONSECUTIVO")).count());
        assertTrue(errores.stream().anyMatch(e -> e.ruta().endsWith(".enunciado")));
        assertTrue(errores.stream().anyMatch(e -> e.ruta().endsWith(".texto")));
        assertTrue(errores.stream().anyMatch(e -> e.ruta().endsWith(".justificacion")));
    }

    @Test
    @DisplayName("RF-06/07: cada escenario necesita preguntas y cada pregunta opciones")
    void rechazaColeccionesIncompletas() {
        var caso = new ConstructorCasoAcademico(UUID.randomUUID(), UUID.randomUUID())
                .agregarEscenario(new Escenario(UUID.randomUUID(), "Uno", "Contexto", 1, List.of()))
                .agregarEscenario(new Escenario(UUID.randomUUID(), "Dos", "Contexto", 2,
                        List.of(new Pregunta(UUID.randomUUID(), "Pregunta", 1, List.of())))).construir();
        var codigos = validacion.validar(caso).errores().stream().map(ErrorValidacion::codigo).toList();
        assertTrue(codigos.contains("PREGUNTAS_REQUERIDAS"));
        assertTrue(codigos.contains("OPCIONES_REQUERIDAS"));
    }

    @Test
    @DisplayName("RF-17 / RN-03: ponderaciones fraccionarias deben sumar 1,00")
    void compruebaSumaExactaSinToleranciasDeComaFlotante() {
        Caso caso = DatosPruebaCasos.completo(UUID.randomUUID());
        UUID pregunta = caso.escenarios().getFirst().preguntas().getFirst().id();
        for (String[] pesos : new String[][]{{"0.33","0.67"}, {"0.10","0.90"}, {"0.50","0.50"}}) {
            var rubrica = new Rubrica(UUID.randomUUID(), "Rubrica", BigDecimal.TEN, List.of(
                    new CriterioRubrica(UUID.randomUUID(), "Uno", new BigDecimal(pesos[0]), List.of(pregunta)),
                    new CriterioRubrica(UUID.randomUUID(), "Dos", new BigDecimal(pesos[1]), List.of(pregunta))));
            assertTrue(validacion.validar(DatosPruebaCasos.conRubrica(caso, rubrica)).esValido());
        }
        for (String peso : List.of("0.99", "1.01", "100.00")) {
            var rubrica = new Rubrica(UUID.randomUUID(), "Rubrica", BigDecimal.TEN, List.of(
                    new CriterioRubrica(UUID.randomUUID(), "Uno", new BigDecimal(peso), List.of(pregunta))));
            assertTrue(validacion.validar(DatosPruebaCasos.conRubrica(caso, rubrica)).errores().stream()
                    .anyMatch(e -> e.codigo().equals("PONDERACIONES_INVALIDAS")));
        }
    }

    @Test
    @DisplayName("RF-17: rubrica debe tener criterios y cubrir las preguntas del caso")
    void detectaRubricaIncompleta() {
        Caso caso = DatosPruebaCasos.completo(UUID.randomUUID());
        var rubrica = new Rubrica(UUID.randomUUID(), "", BigDecimal.ONE, List.of(
                new CriterioRubrica(UUID.randomUUID(), "", BigDecimal.ONE, List.of())));
        var codigos = validacion.validar(DatosPruebaCasos.conRubrica(caso, rubrica)).errores().stream()
                .map(ErrorValidacion::codigo).toList();
        assertTrue(codigos.contains("TEXTO_REQUERIDO"));
        assertTrue(codigos.contains("CRITERIO_SIN_PREGUNTAS"));
        assertTrue(codigos.contains("PREGUNTA_SIN_CRITERIO"));
        var vacia = new Rubrica(UUID.randomUUID(), "Rubrica", BigDecimal.ONE, List.of());
        assertTrue(validacion.validar(DatosPruebaCasos.conRubrica(caso, vacia)).errores().stream()
                .anyMatch(e -> e.codigo().equals("CRITERIOS_REQUERIDOS")));
    }

    @Test
    @DisplayName("RN-11: no inventa un minimo de dos opciones pendiente de aprobacion")
    void noImponeMinimosNoAprobados() {
        Caso base = DatosPruebaCasos.completo(UUID.randomUUID());
        var pregunta = base.escenarios().getFirst().preguntas().getFirst();
        var escenario = new Escenario(UUID.randomUUID(), "Titulo", "Contexto", 1, List.of(
                new Pregunta(pregunta.id(), pregunta.enunciado(), 1, List.of(pregunta.opciones().getFirst()))));
        var caso = new Caso(base.id(), base.idDocente(), base.nombre(), base.desarrolloSituacional(), base.contexto(),
                base.tiempoEstimadoMin(), List.of(escenario), base.rubrica());
        assertTrue(validacion.validar(caso).esValido());
    }
}
