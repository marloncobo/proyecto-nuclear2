package com.psicoapp.cases.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CriterioRubrica(UUID id, String nombre, BigDecimal ponderacion, List<UUID> preguntas) {
    public CriterioRubrica {
        ContenidoCaso.identificador(id);
        nombre = ContenidoCaso.texto(nombre, 120);
        ContenidoCaso.decimal(ponderacion, 3, 2, true);
        preguntas = ContenidoCaso.copiar(preguntas).stream().sorted().toList();
        ContenidoCaso.unicos(preguntas);
    }
}
