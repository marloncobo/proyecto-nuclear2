package com.psicoapp.cases.domain;

import java.util.List;
import java.util.UUID;

public record Pregunta(UUID id, String enunciado, int orden, List<OpcionRespuesta> opciones) {
    public Pregunta {
        ContenidoCaso.identificador(id);
        ContenidoCaso.orden(orden);
        enunciado = ContenidoCaso.texto(enunciado, 16000);
        opciones = ContenidoCaso.copiar(opciones).stream()
                .sorted(java.util.Comparator.comparing(OpcionRespuesta::id)).toList();
        ContenidoCaso.unicos(opciones.stream().map(OpcionRespuesta::id).toList());
    }
}
