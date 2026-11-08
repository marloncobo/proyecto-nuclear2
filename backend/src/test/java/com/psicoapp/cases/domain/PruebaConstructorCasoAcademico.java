package com.psicoapp.cases.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PruebaConstructorCasoAcademico {
    private ConstructorCaso nuevo() {
        return new ConstructorCasoAcademico(UUID.randomUUID(), UUID.randomUUID());
    }

    private Escenario escenario(int orden, List<Pregunta> preguntas) {
        return new Escenario(UUID.randomUUID(), "Escenario", "Contexto", orden, preguntas);
    }

    @Test
    @DisplayName("RF-05 / RF-09: construye borrador incompleto sin publicarlo")
    void permiteBorradorSinEscenariosNiRubrica() {
        Caso caso = nuevo().construir();
        assertEquals("BORRADOR", caso.estado());
        assertEquals("", caso.nombre());
        assertNull(caso.tiempoEstimadoMin());
        assertTrue(caso.escenarios().isEmpty());
        assertNull(caso.rubrica());
    }

    @Test
    @DisplayName("RF-05/06/07/08/17, RN-02/11: ensambla contenido y rubrica en orden")
    void ensamblaCasoCompletoConColeccionesInmutables() {
        var opcion = new OpcionRespuesta(UUID.randomUUID(), "Escuchar", true, "Considera perspectivas");
        var pregunta = new Pregunta(UUID.randomUUID(), "Como intervenir", 1, List.of(opcion));
        var criterio = new CriterioRubrica(UUID.randomUUID(), "Analisis", new BigDecimal("1.00"), List.of(pregunta.id()));
        var rubrica = new Rubrica(UUID.randomUUID(), "Rubrica", new BigDecimal("5.00"), List.of(criterio));
        var primero = escenario(1, List.of(pregunta));
        var segundo = escenario(2, List.of());
        ConstructorCaso constructor = nuevo().definirDatos(" Caso ", "Desarrollo", "Historia", 40)
                .agregarEscenario(segundo).agregarEscenario(primero).asociarRubrica(rubrica);
        Caso caso = constructor.construir();
        assertEquals("Caso", caso.nombre());
        assertEquals(List.of(primero, segundo), caso.escenarios());
        assertEquals(opcion, caso.escenarios().getFirst().preguntas().getFirst().opciones().getFirst());
        assertEquals(rubrica, caso.rubrica());
        assertThrows(UnsupportedOperationException.class, () -> caso.escenarios().clear());
        constructor.agregarEscenario(escenario(3, List.of()));
        assertEquals(2, caso.escenarios().size());
        assertEquals("BORRADOR", caso.estado());
    }

    @Test
    @DisplayName("RN-02: rechaza posiciones duplicadas y negativas")
    void rechazaOrdenInvalido() {
        assertThrows(IllegalArgumentException.class, () -> escenario(0, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                nuevo().agregarEscenario(escenario(1, List.of())).agregarEscenario(escenario(1, List.of())).construir());
        var pregunta = new Pregunta(UUID.randomUUID(), "", 1, List.of());
        assertThrows(IllegalArgumentException.class, () -> escenario(1,
                List.of(pregunta, new Pregunta(UUID.randomUUID(), "", 1, List.of()))));
    }

    @Test
    @DisplayName("RF-06 / DSL: detecta identificadores repetidos entre escenarios")
    void rechazaPreguntasYOpcionesCompartidasEntreEscenarios() {
        var opcion = new OpcionRespuesta(UUID.randomUUID(), "", false, "");
        var pregunta = new Pregunta(UUID.randomUUID(), "", 1, List.of(opcion));
        assertThrows(IllegalArgumentException.class, () -> nuevo()
                .agregarEscenario(escenario(1, List.of(pregunta)))
                .agregarEscenario(escenario(2, List.of(pregunta))).construir());
        var otra = new Pregunta(UUID.randomUUID(), "", 1, List.of(opcion));
        assertThrows(IllegalArgumentException.class, () -> nuevo()
                .agregarEscenario(escenario(1, List.of(pregunta)))
                .agregarEscenario(escenario(2, List.of(otra))).construir());
    }

    @Test
    @DisplayName("RF-17 / ER CRITERIO_PREGUNTA: no permite referencias ajenas")
    void rechazaRubricaConPreguntaInexistente() {
        var criterio = new CriterioRubrica(UUID.randomUUID(), "", BigDecimal.ONE, List.of(UUID.randomUUID()));
        var rubrica = new Rubrica(UUID.randomUUID(), "", BigDecimal.ONE, List.of(criterio));
        assertThrows(IllegalArgumentException.class, () -> nuevo().asociarRubrica(rubrica).construir());
    }

    @Test
    @DisplayName("RF-05 / RF-17: invalida tiempo, longitud y decimales que perderian precision")
    void rechazaValoresFueraDelModelo() {
        assertThrows(IllegalArgumentException.class, () -> nuevo().definirDatos("", "", "", 0).construir());
        assertThrows(IllegalArgumentException.class, () -> nuevo().definirDatos("x".repeat(181), "", "", 1).construir());
        assertThrows(IllegalArgumentException.class, () -> new Rubrica(UUID.randomUUID(), "", BigDecimal.ZERO, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new CriterioRubrica(UUID.randomUUID(), "", new BigDecimal("0.001"), List.of()));
        var opciones = new ArrayList<OpcionRespuesta>();
        var pregunta = new Pregunta(UUID.randomUUID(), "", 1, opciones);
        opciones.add(new OpcionRespuesta(UUID.randomUUID(), "", false, ""));
        assertTrue(pregunta.opciones().isEmpty());
    }
}
