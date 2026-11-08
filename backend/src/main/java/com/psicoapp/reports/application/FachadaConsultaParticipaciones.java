package com.psicoapp.reports.application;

import com.psicoapp.evaluation.application.ConsultaResultados;
import com.psicoapp.evaluation.application.GestionValoraciones;
import com.psicoapp.evaluation.application.GestionValoraciones.ValoracionPublicada;
import com.psicoapp.evaluation.application.ConsultaResultados.ResultadoPublicado;
import com.psicoapp.participation.application.ConsultaParticipaciones;
import com.psicoapp.participation.application.ConsultaParticipaciones.ParticipacionFinalizada;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Facade de seguimiento: coordina contratos publicos sin acceder a repositorios ajenos. */
@Service
@Transactional(readOnly = true)
public class FachadaConsultaParticipaciones {
    /** Resultado nulo significa evaluacion pendiente, nunca una calificacion de cero. */
    public record ResumenParticipacion(ParticipacionFinalizada participacion, ResultadoPublicado resultado,
            List<ValoracionPublicada> valoracionesGenerales) {
        public ResumenParticipacion { valoracionesGenerales = List.copyOf(valoracionesGenerales); }
    }

    private final ConsultaParticipaciones participaciones;
    private final ConsultaResultados resultados;
    private final GestionValoraciones valoraciones;

    public FachadaConsultaParticipaciones(ConsultaParticipaciones participaciones, ConsultaResultados resultados, GestionValoraciones valoraciones) {
        this.participaciones = participaciones;
        this.resultados = resultados;
        this.valoraciones = valoraciones;
    }

    public List<ResumenParticipacion> consultarPorEstudiante(UUID idDocente, UUID idEstudiante, UUID idCaso) {
        return participaciones.consultarPorEstudiante(idDocente, idEstudiante, idCaso).stream()
                .map(participacion -> new ResumenParticipacion(participacion,
                        resultados.consultar(idDocente, participacion.idIntento()).orElse(null),
                        valoraciones.consultar(idDocente, participacion.idIntento())))
                .toList();
    }

    /** CU-32 incluye CU-33: el modulo propietario guarda y solicita un solo aviso. */
    @Transactional
    public ValoracionPublicada registrarValoracion(UUID idDocente, UUID idIntento, String mensaje) {
        return valoraciones.registrar(idDocente, idIntento, mensaje);
    }
}
