package com.psicoapp.cases;

import com.psicoapp.cases.domain.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class DatosPruebaCasos {
    private DatosPruebaCasos() { }

    public static Caso completo(UUID docente) {
        var pregunta = new Pregunta(UUID.randomUUID(), "Como abordar el conflicto", 1, List.of(
                new OpcionRespuesta(UUID.randomUUID(), "Escuchar", true, "Reconoce perspectivas"),
                new OpcionRespuesta(UUID.randomUUID(), "Imponer", false, "Ignora intereses")));
        var escenario = new Escenario(UUID.randomUUID(), "Reunion", "Desacuerdo en el equipo", 1, List.of(pregunta));
        var criterio = new CriterioRubrica(UUID.randomUUID(), "Analisis", new BigDecimal("1.00"), List.of(pregunta.id()));
        return new ConstructorCasoAcademico(UUID.randomUUID(), docente)
                .definirDatos("Conflicto", "Desarrollo situacional", "Contexto educativo", 40)
                .agregarEscenario(escenario)
                .asociarRubrica(new Rubrica(UUID.randomUUID(), "Rubrica", new BigDecimal("5.00"), List.of(criterio)))
                .construir();
    }

    public static Caso conRubrica(Caso caso, Rubrica rubrica) {
        return new Caso(caso.id(), caso.idDocente(), caso.nombre(), caso.desarrolloSituacional(),
                caso.contexto(), caso.tiempoEstimadoMin(), caso.escenarios(), rubrica);
    }
}
