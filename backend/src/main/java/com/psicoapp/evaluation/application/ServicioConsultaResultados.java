package com.psicoapp.evaluation.application;

import com.psicoapp.participation.application.ConsultaParticipaciones;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServicioConsultaResultados implements ConsultaResultados {
    private final ConsultaParticipaciones participaciones;
    private final RepositorioEvaluaciones evaluaciones;

    public ServicioConsultaResultados(ConsultaParticipaciones participaciones, RepositorioEvaluaciones evaluaciones) {
        this.participaciones = participaciones;
        this.evaluaciones = evaluaciones;
    }

    @Override
    public Optional<ResultadoPublicado> consultar(UUID idDocente, UUID idIntento) {
        participaciones.exigirFinalizada(idDocente, idIntento);
        return evaluaciones.buscarPorIntento(idIntento).map(evaluacion -> new ResultadoPublicado(
                evaluacion.resultado().calificacion(), evaluacion.fecha(),
                evaluacion.resultado().detalles().stream().map(detalle -> new RespuestaEvaluada(
                        detalle.idPregunta(), detalle.idOpcion(), detalle.contestada(), detalle.correcta(),
                        detalle.puntos(), detalle.retroalimentacion())).toList()));
    }
}
