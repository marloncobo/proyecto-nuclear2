package com.psicoapp.shared.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorExcepcionesApi {
    @ExceptionHandler(AccesoDenegado.class)
    ProblemDetail accesoDenegado(AccesoDenegado excepcion) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, excepcion.getMessage());
    }

    @ExceptionHandler(NoEncontrado.class)
    ProblemDetail noEncontrado(NoEncontrado excepcion) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excepcion.getMessage());
    }

    @ExceptionHandler(Conflicto.class)
    ProblemDetail conflicto(Conflicto excepcion) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excepcion.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Los datos entran en conflicto con un registro existente");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ProblemDetail solicitudInvalida() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Solicitud inválida: revise los campos enviados");
    }
}
