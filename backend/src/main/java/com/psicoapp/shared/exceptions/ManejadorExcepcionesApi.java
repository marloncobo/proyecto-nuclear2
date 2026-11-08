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

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail dominioInvalido(IllegalArgumentException excepcion) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, excepcion.getMessage());
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ProblemDetail cuerpoInvalido(org.springframework.http.converter.HttpMessageNotReadableException excepcion) {
        Throwable causa = excepcion.getCause();
        while (causa != null) {
            if (causa instanceof IllegalArgumentException invalido) return dominioInvalido(invalido);
            causa = causa.getCause();
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "JSON invalido o incompatible con los campos esperados");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ProblemDetail solicitudInvalida() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Solicitud inválida: revise los campos enviados");
    }
}
