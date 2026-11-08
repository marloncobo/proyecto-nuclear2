package com.psicoapp.evaluation.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public record PreguntaEvaluable(UUID id, List<OpcionEvaluable> opciones) {
    public PreguntaEvaluable {
        Objects.requireNonNull(id, "La pregunta es obligatoria");
        if (opciones == null || opciones.isEmpty()) throw new IllegalArgumentException("La pregunta requiere opciones");
        if (opciones.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("La opcion es obligatoria");
        Map<UUID, OpcionEvaluable> unicas = opciones.stream()
                .collect(Collectors.toMap(OpcionEvaluable::id, Function.identity(), (primera, segunda) -> {
                    throw new IllegalArgumentException("Las opciones no pueden repetirse");
                }));
        opciones = List.copyOf(unicas.values());
    }

    public OpcionEvaluable opcion(UUID idOpcion) {
        return opciones.stream().filter(opcion -> opcion.id().equals(idOpcion)).findFirst().orElse(null);
    }
}
