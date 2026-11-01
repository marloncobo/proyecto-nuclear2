package com.psicoapp.evaluation.application;

import com.psicoapp.evaluation.domain.PreguntaEvaluable;
import com.psicoapp.evaluation.domain.RubricaEvaluable;
import java.util.List;
import java.util.UUID;

public interface ProveedorCasosEvaluables {
    record CasoEvaluable(List<PreguntaEvaluable> preguntas, RubricaEvaluable rubrica) { }
    CasoEvaluable buscarPorSesion(UUID idSesion);
}
