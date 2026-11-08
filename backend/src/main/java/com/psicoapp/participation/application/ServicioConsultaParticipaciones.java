package com.psicoapp.participation.application;

import com.psicoapp.cases.application.ConsultaCasosDocente;
import com.psicoapp.participation.domain.EstadoIntento;
import com.psicoapp.participation.domain.Intento;
import com.psicoapp.sessions.application.ConsultaSesionesDocente;
import com.psicoapp.shared.exceptions.Conflicto;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServicioConsultaParticipaciones implements ConsultaParticipaciones {
    private final RepositorioConsultaIntentos consultas;
    private final RepositorioIntentos intentos;
    private final ConsultaCasosDocente casos;
    private final ConsultaSesionesDocente sesiones;

    public ServicioConsultaParticipaciones(RepositorioConsultaIntentos consultas, RepositorioIntentos intentos,
            ConsultaCasosDocente casos, ConsultaSesionesDocente sesiones) {
        this.consultas = consultas;
        this.intentos = intentos;
        this.casos = casos;
        this.sesiones = sesiones;
    }

    @Override
    public List<ParticipacionFinalizada> consultarPorEstudiante(UUID idDocente, UUID idEstudiante, UUID idCaso) {
        // Reutiliza la comprobacion de rol y propiedad, incluso para casos archivados.
        casos.exigirResponsable(idDocente, idCaso);
        return consultas.buscarTerminadosPorEstudianteYCaso(idEstudiante, idCaso).stream()
                .map(this::representar).toList();
    }

    @Override
    public ParticipacionFinalizada exigirFinalizada(UUID idDocente, UUID idIntento) {
        Intento intento = intentos.buscarPorId(idIntento)
                .orElseThrow(() -> new NoEncontrado("Intento no encontrado"));
        sesiones.exigirResponsable(idDocente, intento.idSesion());
        return representar(intento);
    }

    private ParticipacionFinalizada representar(Intento intento) {
        if (intento.estado() == EstadoIntento.EN_PROGRESO) {
            throw new Conflicto("La participacion aun no ha finalizado");
        }
        return new ParticipacionFinalizada(intento.id(), intento.idSesion(), intento.idEstudiante(),
                intento.estado().name(), intento.inicio(), intento.limite(), intento.preguntas(), intento.respuestas());
    }
}
