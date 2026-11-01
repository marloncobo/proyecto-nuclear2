package com.psicoapp.cases.domain;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Agregado academico inmutable. Todo caso de esta entrega permanece en BORRADOR. */
public record Caso(UUID id, UUID idDocente, String nombre, String desarrolloSituacional, String contexto,
        Integer tiempoEstimadoMin, List<Escenario> escenarios, Rubrica rubrica) {
    public Caso {
        ContenidoCaso.identificador(id);
        ContenidoCaso.identificador(idDocente);
        nombre = ContenidoCaso.texto(nombre, 180);
        desarrolloSituacional = ContenidoCaso.texto(desarrolloSituacional, 16000);
        contexto = ContenidoCaso.texto(contexto, 16000);
        if (tiempoEstimadoMin != null && tiempoEstimadoMin <= 0) {
            throw new IllegalArgumentException("El tiempo estimado debe ser positivo");
        }
        escenarios = ContenidoCaso.copiar(escenarios).stream().sorted(Comparator.comparingInt(Escenario::orden)).toList();
        ContenidoCaso.unicos(escenarios.stream().map(Escenario::id).toList());
        ContenidoCaso.unicos(escenarios.stream().map(Escenario::orden).toList());
        var preguntas = escenarios.stream().flatMap(e -> e.preguntas().stream()).toList();
        ContenidoCaso.unicos(preguntas.stream().map(Pregunta::id).toList());
        ContenidoCaso.unicos(preguntas.stream().flatMap(p -> p.opciones().stream()).map(OpcionRespuesta::id).toList());
        if (rubrica != null) {
            var identificadores = preguntas.stream().map(Pregunta::id).collect(java.util.stream.Collectors.toSet());
            if (rubrica.criterios().stream().flatMap(c -> c.preguntas().stream()).anyMatch(idPregunta -> !identificadores.contains(idPregunta))) {
                throw new IllegalArgumentException("La rubrica referencia preguntas ajenas al caso");
            }
        }
    }

    public String estado() {
        return "BORRADOR";
    }
}
