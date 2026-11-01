package com.psicoapp.cases.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record Rubrica(UUID id, String nombre, BigDecimal escalaMaxima, List<CriterioRubrica> criterios) {
    public Rubrica {
        ContenidoCaso.identificador(id);
        nombre = ContenidoCaso.texto(nombre, 120);
        ContenidoCaso.decimal(escalaMaxima, 3, 2, false);
        criterios = ContenidoCaso.copiar(criterios).stream()
                .sorted(java.util.Comparator.comparing(CriterioRubrica::id)).toList();
        ContenidoCaso.unicos(criterios.stream().map(CriterioRubrica::id).toList());
    }
}
