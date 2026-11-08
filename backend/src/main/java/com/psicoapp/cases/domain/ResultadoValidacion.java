package com.psicoapp.cases.domain;

import java.util.List;

public record ResultadoValidacion(List<ErrorValidacion> errores) {
    public ResultadoValidacion {
        errores = List.copyOf(errores);
    }

    public boolean esValido() {
        return errores.isEmpty();
    }
}
