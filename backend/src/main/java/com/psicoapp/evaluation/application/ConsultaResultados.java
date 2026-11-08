package com.psicoapp.evaluation.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Resultados almacenados de intentos terminados; no calcula ni modifica notas. */
public interface ConsultaResultados {
    record RespuestaEvaluada(UUID idPregunta, UUID idOpcion, boolean contestada, boolean correcta,
            BigDecimal puntos, String retroalimentacion) { }

    record ResultadoPublicado(BigDecimal calificacion, Instant fecha, List<RespuestaEvaluada> detalles) {
        public ResultadoPublicado {
            detalles = List.copyOf(detalles);
        }
    }

    Optional<ResultadoPublicado> consultar(UUID idDocente, UUID idIntento);
}
