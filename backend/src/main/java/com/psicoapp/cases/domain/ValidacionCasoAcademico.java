package com.psicoapp.cases.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** RF-09, RN-03/05/11: reglas de publicacion del modelo academico vigente. */
public final class ValidacionCasoAcademico extends PlantillaValidacionPublicacion {
    @Override
    protected void validarEstructura(Caso caso, List<ErrorValidacion> errores) {
        exigirTexto(caso.nombre(), "nombre", errores);
        exigirTexto(caso.desarrolloSituacional(), "desarrolloSituacional", errores);
        exigirTexto(caso.contexto(), "contexto", errores);
        if (caso.tiempoEstimadoMin() == null) {
            agregar(errores, "TIEMPO_REQUERIDO", "tiempoEstimadoMin", "Debe definir el tiempo estimado");
        }
        if (caso.escenarios().isEmpty()) {
            agregar(errores, "ESCENARIOS_REQUERIDOS", "escenarios", "Debe registrar escenarios academicos");
        }
        for (int indice = 0; indice < caso.escenarios().size(); indice++) {
            Escenario escenario = caso.escenarios().get(indice);
            String ruta = "escenarios[" + indice + "]";
            exigirTexto(escenario.titulo(), ruta + ".titulo", errores);
            exigirTexto(escenario.contextoSituacional(), ruta + ".contextoSituacional", errores);
            if (escenario.orden() != indice + 1) {
                agregar(errores, "ORDEN_NO_CONSECUTIVO", ruta + ".orden", "El orden debe ser consecutivo desde 1");
            }
        }
    }

    @Override
    protected void validarPreguntas(Caso caso, List<ErrorValidacion> errores) {
        for (int indice = 0; indice < caso.escenarios().size(); indice++) {
            Escenario escenario = caso.escenarios().get(indice);
            String rutaEscenario = "escenarios[" + indice + "]";
            if (escenario.preguntas().isEmpty()) {
                agregar(errores, "PREGUNTAS_REQUERIDAS", rutaEscenario + ".preguntas", "El escenario debe contener preguntas");
            }
            for (int posicion = 0; posicion < escenario.preguntas().size(); posicion++) {
                Pregunta pregunta = escenario.preguntas().get(posicion);
                String ruta = rutaEscenario + ".preguntas[" + posicion + "]";
                exigirTexto(pregunta.enunciado(), ruta + ".enunciado", errores);
                if (pregunta.orden() != posicion + 1) {
                    agregar(errores, "ORDEN_NO_CONSECUTIVO", ruta + ".orden", "El orden debe ser consecutivo desde 1");
                }
                if (pregunta.opciones().isEmpty()) {
                    agregar(errores, "OPCIONES_REQUERIDAS", ruta + ".opciones", "La pregunta debe contener opciones");
                }
                for (int opcion = 0; opcion < pregunta.opciones().size(); opcion++) {
                    OpcionRespuesta respuesta = pregunta.opciones().get(opcion);
                    String rutaOpcion = ruta + ".opciones[" + opcion + "]";
                    exigirTexto(respuesta.texto(), rutaOpcion + ".texto", errores);
                    exigirTexto(respuesta.justificacion(), rutaOpcion + ".justificacion", errores);
                }
            }
        }
    }

    @Override
    protected void validarRubrica(Caso caso, List<ErrorValidacion> errores) {
        Rubrica rubrica = caso.rubrica();
        if (rubrica == null) {
            agregar(errores, "RUBRICA_REQUERIDA", "rubrica", "Debe asociar una rubrica");
            return;
        }
        exigirTexto(rubrica.nombre(), "rubrica.nombre", errores);
        if (rubrica.criterios().isEmpty()) {
            agregar(errores, "CRITERIOS_REQUERIDOS", "rubrica.criterios", "La rubrica debe contener criterios");
        }
        BigDecimal total = BigDecimal.ZERO;
        Set<UUID> cubiertas = new HashSet<>();
        for (int indice = 0; indice < rubrica.criterios().size(); indice++) {
            CriterioRubrica criterio = rubrica.criterios().get(indice);
            String ruta = "rubrica.criterios[" + indice + "]";
            exigirTexto(criterio.nombre(), ruta + ".nombre", errores);
            total = total.add(criterio.ponderacion());
            if (criterio.ponderacion().compareTo(BigDecimal.ONE) > 0) {
                agregar(errores, "PONDERACION_FUERA_DE_RANGO", ruta + ".ponderacion", "La ponderacion debe estar entre 0 y 1");
            }
            if (criterio.preguntas().isEmpty()) {
                agregar(errores, "CRITERIO_SIN_PREGUNTAS", ruta + ".preguntas", "Debe asociar preguntas al criterio");
            }
            // Las referencias ajenas y duplicadas se rechazan al construir el agregado.
            cubiertas.addAll(criterio.preguntas());
        }
        if (total.compareTo(BigDecimal.ONE) != 0) {
            agregar(errores, "PONDERACIONES_INVALIDAS", "rubrica.criterios", "Las ponderaciones deben sumar 1,00");
        }
        for (int indice = 0; indice < caso.escenarios().size(); indice++) {
            Escenario escenario = caso.escenarios().get(indice);
            for (int posicion = 0; posicion < escenario.preguntas().size(); posicion++) {
                Pregunta pregunta = escenario.preguntas().get(posicion);
                if (!cubiertas.contains(pregunta.id())) {
                    agregar(errores, "PREGUNTA_SIN_CRITERIO", "escenarios[" + indice + "].preguntas[" + posicion + "]",
                            "Cada pregunta debe estar asociada a la rubrica");
                }
            }
        }
    }

    private void exigirTexto(String texto, String ruta, List<ErrorValidacion> errores) {
        if (texto.isBlank()) agregar(errores, "TEXTO_REQUERIDO", ruta, "Debe completar este contenido");
    }

    private void agregar(List<ErrorValidacion> errores, String codigo, String ruta, String mensaje) {
        errores.add(new ErrorValidacion(codigo, ruta, mensaje));
    }
}
