package com.psicoapp.evaluation.application;

import com.psicoapp.evaluation.domain.ResultadoEvaluacion;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioEvaluaciones {
    record EvaluacionPersistida(UUID idIntento, ResultadoEvaluacion resultado, Instant fecha) { }
    void guardar(EvaluacionPersistida evaluacion);
    Optional<EvaluacionPersistida> buscarPorIntento(UUID idIntento);
}
