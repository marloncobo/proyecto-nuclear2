package com.psicoapp.participation.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Consulta docente de intentos terminados, sin exponer entidades de dominio. */
public interface ConsultaParticipaciones {
    record ParticipacionFinalizada(UUID idIntento, UUID idSesion, UUID idEstudiante,
            String estado, Instant inicio, Instant limite, List<UUID> preguntas, Map<UUID, UUID> respuestas) {
        public ParticipacionFinalizada {
            preguntas = List.copyOf(preguntas);
            respuestas = Map.copyOf(respuestas);
        }
    }

    List<ParticipacionFinalizada> consultarPorEstudiante(UUID idDocente, UUID idEstudiante, UUID idCaso);
    ParticipacionFinalizada exigirFinalizada(UUID idDocente, UUID idIntento);
}
