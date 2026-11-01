package com.psicoapp.cases.application;

import com.psicoapp.cases.domain.ResultadoValidacion;

public final class PublicacionInvalida extends RuntimeException {
    private final ResultadoValidacion resultado;

    public PublicacionInvalida(ResultadoValidacion resultado) {
        super("El caso no cumple las condiciones de publicacion");
        this.resultado = resultado;
    }

    public ResultadoValidacion resultado() {
        return resultado;
    }
}
