package com.psicoapp.cases.api;

import com.psicoapp.cases.application.PublicacionInvalida;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ControladorPublicacionCasos.class)
public class ManejadorPublicacionCasos {
    @ExceptionHandler(PublicacionInvalida.class)
    ProblemDetail publicacionInvalida(PublicacionInvalida excepcion) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, excepcion.getMessage());
        problema.setProperty("errores", excepcion.resultado().errores());
        return problema;
    }
}
