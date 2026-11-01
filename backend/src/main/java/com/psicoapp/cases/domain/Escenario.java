package com.psicoapp.cases.domain;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record Escenario(UUID id, String titulo, String contextoSituacional, int orden, List<Pregunta> preguntas) {
    public Escenario {
        ContenidoCaso.identificador(id);
        ContenidoCaso.orden(orden);
        titulo = ContenidoCaso.texto(titulo, 180);
        contextoSituacional = ContenidoCaso.texto(contextoSituacional, 16000);
        preguntas = ContenidoCaso.copiar(preguntas).stream().sorted(Comparator.comparingInt(Pregunta::orden)).toList();
        ContenidoCaso.unicos(preguntas.stream().map(Pregunta::id).toList());
        ContenidoCaso.unicos(preguntas.stream().map(Pregunta::orden).toList());
    }
}
