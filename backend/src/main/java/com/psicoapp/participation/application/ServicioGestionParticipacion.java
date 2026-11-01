package com.psicoapp.participation.application;

import com.psicoapp.sessions.application.AutorizacionConvocatorias;
import com.psicoapp.sessions.application.ConsultaSesiones;
import com.psicoapp.participation.domain.Intento;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.Conflicto;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioGestionParticipacion {
    public record DatosIntento(UUID id, UUID idSesion, UUID idEstudiante, Instant inicio, Instant limite,
            String estado, List<UUID> preguntas, UUID preguntaActual, java.util.Map<UUID, UUID> respuestas) { }

    private final RepositorioIntentos intentos;
    private final AutorizacionConvocatorias autorizacion;
    private final ConsultaSesiones sesiones;
    private final Clock reloj;

    public ServicioGestionParticipacion(RepositorioIntentos intentos, AutorizacionConvocatorias autorizacion,
            ConsultaSesiones sesiones, Clock reloj) {
        this.intentos = intentos;
        this.autorizacion = autorizacion;
        this.sesiones = sesiones;
        this.reloj = reloj;
    }

    public DatosIntento iniciar(UUID idEstudiante, UUID idSesion, String codigo, List<UUID> preguntas) {
        autorizacion.exigirAcceso(idEstudiante, idSesion, codigo);
        ConsultaSesiones.SesionEnCurso sesion = sesiones.exigirEnCurso(idSesion);
        if (intentos.buscarActivo(idSesion, idEstudiante).isPresent()) {
            throw new Conflicto("El estudiante ya tiene un intento en progreso");
        }
        Intento intento = Intento.iniciar(UUID.randomUUID(), idSesion, idEstudiante, reloj.instant(),
                sesion.duracionMaximaMin(), com.psicoapp.sessions.domain.ModoNavegacion.valueOf(sesion.modoNavegacion()),
                preguntas);
        intentos.guardar(intento);
        return representar(intento);
    }

    @Transactional(readOnly = true)
    public DatosIntento consultar(UUID idEstudiante, UUID idIntento) {
        return representar(exigirPropietario(idEstudiante, idIntento));
    }

    public DatosIntento responder(UUID idEstudiante, UUID idIntento, UUID idPregunta, UUID idOpcion) {
        Intento intento = bloquearPropietario(idEstudiante, idIntento);
        Intento actualizado = intento.responder(idPregunta, idOpcion, reloj.instant());
        intentos.actualizar(actualizado);
        return representar(actualizado);
    }

    public DatosIntento avanzar(UUID idEstudiante, UUID idIntento) {
        Intento intento = bloquearPropietario(idEstudiante, idIntento);
        Intento actualizado = intento.avanzar(reloj.instant());
        intentos.actualizar(actualizado);
        return representar(actualizado);
    }

    public DatosIntento retroceder(UUID idEstudiante, UUID idIntento) {
        Intento intento = bloquearPropietario(idEstudiante, idIntento);
        Intento actualizado = intento.retroceder(reloj.instant());
        intentos.actualizar(actualizado);
        return representar(actualizado);
    }

    public DatosIntento reanudar(UUID idEstudiante, UUID idIntento) {
        Intento intento = bloquearPropietario(idEstudiante, idIntento);
        return representar(intento.reanudar(reloj.instant()));
    }

    public DatosIntento finalizar(UUID idEstudiante, UUID idIntento) {
        Intento intento = bloquearPropietario(idEstudiante, idIntento);
        Intento actualizado = intento.finalizar(reloj.instant());
        intentos.actualizar(actualizado);
        return representar(actualizado);
    }

    public int finalizarVencidos(Instant ahora) {
        if (ahora == null) throw new IllegalArgumentException("El instante es obligatorio");
        int finalizados = 0;
        for (Intento candidato : intentos.buscarVencidos(ahora)) {
            Intento intento = intentos.bloquearPorId(candidato.id()).orElseThrow(
                    () -> new NoEncontrado("Intento no encontrado"));
            if (intento.estaVencido(ahora) && intento.estado()
                    == com.psicoapp.participation.domain.EstadoIntento.EN_PROGRESO) {
                intentos.actualizar(intento.vencer(ahora));
                finalizados++;
            }
        }
        return finalizados;
    }

    private Intento bloquearPropietario(UUID idEstudiante, UUID idIntento) {
        Intento intento = intentos.bloquearPorId(idIntento).orElseThrow(
                () -> new NoEncontrado("Intento no encontrado"));
        exigirPropietario(idEstudiante, intento);
        return intento;
    }

    private Intento exigirPropietario(UUID idEstudiante, UUID idIntento) {
        return intentos.buscarPorId(idIntento).map(intento -> {
            exigirPropietario(idEstudiante, intento);
            return intento;
        }).orElseThrow(() -> new NoEncontrado("Intento no encontrado"));
    }

    private void exigirPropietario(UUID idEstudiante, Intento intento) {
        if (!intento.idEstudiante().equals(idEstudiante)) {
            throw new AccesoDenegado("Solo el estudiante propietario puede consultar el intento");
        }
    }

    private DatosIntento representar(Intento intento) {
        return new DatosIntento(intento.id(), intento.idSesion(), intento.idEstudiante(), intento.inicio(),
                intento.limite(), intento.estado().name(), intento.preguntas(), intento.preguntaActualId(),
                intento.respuestas());
    }
}
