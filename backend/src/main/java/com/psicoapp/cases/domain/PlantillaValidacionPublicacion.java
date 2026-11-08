package com.psicoapp.cases.domain;

import java.util.ArrayList;
import java.util.List;

/** Template Method: fija el orden de las etapas y acumula sus errores. */
public abstract class PlantillaValidacionPublicacion {
    public final ResultadoValidacion validar(Caso caso) {
        if (caso == null) throw new IllegalArgumentException("El caso es obligatorio");
        List<ErrorValidacion> errores = new ArrayList<>();
        validarEstructura(caso, errores);
        validarPreguntas(caso, errores);
        validarRubrica(caso, errores);
        return new ResultadoValidacion(errores);
    }

    protected abstract void validarEstructura(Caso caso, List<ErrorValidacion> errores);
    protected abstract void validarPreguntas(Caso caso, List<ErrorValidacion> errores);
    protected abstract void validarRubrica(Caso caso, List<ErrorValidacion> errores);
}
