package com.psicoapp.evaluation.application;

import com.psicoapp.evaluation.domain.EstrategiaCalificacion;
import com.psicoapp.evaluation.domain.ResultadoEvaluacion;
import com.psicoapp.participation.application.RepositorioIntentos;
import com.psicoapp.participation.domain.EstadoIntento;
import com.psicoapp.participation.domain.Intento;
import com.psicoapp.shared.exceptions.Conflicto;
import com.psicoapp.shared.exceptions.NoEncontrado;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioEvaluacion {
    public record DatosEvaluacion(UUID idIntento, String estadoIntento, Instant fecha,
            ResultadoEvaluacion resultado) { }

    private final RepositorioIntentos intentos;
    private final ProveedorCasosEvaluables casos;
    private final RepositorioEvaluaciones evaluaciones;
    private final EstrategiaCalificacion estrategia;
    private final Clock reloj;

    public ServicioEvaluacion(RepositorioIntentos intentos, ProveedorCasosEvaluables casos,
            RepositorioEvaluaciones evaluaciones, EstrategiaCalificacion estrategia, Clock reloj) {
        this.intentos = intentos;
        this.casos = casos;
        this.evaluaciones = evaluaciones;
        this.estrategia = estrategia;
        this.reloj = reloj;
    }

    public DatosEvaluacion evaluar(UUID idEstudiante, UUID idIntento) {
        Intento intento = intentos.buscarPorId(idIntento).orElseThrow(
                () -> new NoEncontrado("Intento no encontrado"));
        exigirPropietario(idEstudiante, intento);
        if (intento.estado() == EstadoIntento.EN_PROGRESO) {
            throw new Conflicto("Solo se pueden evaluar intentos finalizados");
        }
        return evaluaciones.buscarPorIntento(idIntento).map(this::representar).orElseGet(() -> {
            ProveedorCasosEvaluables.CasoEvaluable caso = casos.buscarPorSesion(intento.idSesion());
            ResultadoEvaluacion resultado = estrategia.calificar(caso.preguntas(), intento.respuestas(), caso.rubrica());
            RepositorioEvaluaciones.EvaluacionPersistida guardada =
                    new RepositorioEvaluaciones.EvaluacionPersistida(idIntento, resultado, reloj.instant());
            evaluaciones.guardar(guardada);
            return representar(guardada);
        });
    }

    @Transactional(readOnly = true)
    public DatosEvaluacion consultar(UUID idEstudiante, UUID idIntento) {
        RepositorioEvaluaciones.EvaluacionPersistida evaluacion = evaluaciones.buscarPorIntento(idIntento)
                .orElseThrow(() -> new NoEncontrado("La evaluacion aun no esta disponible"));
        Intento intento = intentos.buscarPorId(idIntento).orElseThrow(
                () -> new NoEncontrado("Intento no encontrado"));
        exigirPropietario(idEstudiante, intento);
        return representar(evaluacion);
    }

    private void exigirPropietario(UUID idEstudiante, Intento intento) {
        if (!intento.idEstudiante().equals(idEstudiante)) {
            throw new AccesoDenegado("Solo el estudiante propietario puede consultar la evaluacion");
        }
    }

    private DatosEvaluacion representar(RepositorioEvaluaciones.EvaluacionPersistida evaluacion) {
        String estado = intentos.buscarPorId(evaluacion.idIntento())
                .map(Intento::estado).map(Enum::name).orElse("DESCONOCIDO");
        return new DatosEvaluacion(evaluacion.idIntento(), estado, evaluacion.fecha(), evaluacion.resultado());
    }
}
