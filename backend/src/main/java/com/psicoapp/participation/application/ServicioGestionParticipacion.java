package com.psicoapp.participation.application;

import com.psicoapp.shared.events.*;
import com.psicoapp.sessions.application.AutorizacionConvocatorias;
import com.psicoapp.sessions.application.ConsultaSesiones;
import com.psicoapp.participation.domain.Intento;
import com.psicoapp.participation.domain.EstadoIntento;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.Conflicto;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioGestionParticipacion {
    public record DatosIntento(UUID id, UUID idSesion, UUID idEstudiante, Instant inicio, Instant limite, Instant fin,
            String estado, String modoNavegacion, List<UUID> preguntas, UUID preguntaActual, java.util.Map<UUID, UUID> respuestas) { }

    private final NotificacionSujeto eventos;
    private final RepositorioIntentos intentos;
    private final AutorizacionConvocatorias autorizacion;
    private final ConsultaSesiones sesiones;
    private final Clock reloj;

    public ServicioGestionParticipacion(RepositorioIntentos intentos, AutorizacionConvocatorias autorizacion,
            ConsultaSesiones sesiones, Clock reloj, NotificacionSujeto eventos) {
        this.eventos = eventos;
        this.intentos = intentos;
        this.autorizacion = autorizacion;
        this.sesiones = sesiones;
        this.reloj = reloj;
    }

    public DatosIntento iniciar(UUID idEstudiante, UUID idSesion, String codigo) {
        autorizacion.exigirAcceso(idEstudiante, idSesion, codigo);
        ConsultaSesiones.SesionEnCurso sesion = sesiones.exigirEnCurso(idSesion);
        if (intentos.buscarActivo(idSesion, idEstudiante).isPresent()) {
            throw new Conflicto("El estudiante ya tiene un intento en progreso");
        }
        Intento intento = Intento.iniciar(UUID.randomUUID(), idSesion, idEstudiante, reloj.instant(),
                sesion.duracionMaximaMin(), com.psicoapp.sessions.domain.ModoNavegacion.valueOf(sesion.modoNavegacion()),
                intentos.preguntasDeSesion(idSesion));
        intentos.guardar(intento);
        return representar(intento);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<DatosIntento> consultarUltimo(UUID estudiante, UUID sesion) {
        return intentos.buscarUltimo(sesion, estudiante).map(this::representar);
    }

    @Transactional(readOnly = true)
    public DatosIntento consultar(UUID idEstudiante, UUID idIntento) {
        return representar(exigirPropietario(idEstudiante, idIntento));
    }

    public DatosIntento responder(UUID idEstudiante, UUID idIntento, UUID idPregunta, UUID idOpcion) {
        Intento intento = bloquearPropietario(idEstudiante, idIntento);
        Intento actualizado = intento.responder(idPregunta, idOpcion, reloj.instant());
        if (!intentos.opcionDePregunta(idPregunta, idOpcion)) {
            throw new IllegalArgumentException("La opcion no pertenece a la pregunta");
        }
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
        publicarFinalizacion(actualizado);
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
                Intento vencido = intento.vencer(ahora);
                intentos.actualizar(vencido);
                publicarFinalizacion(vencido);
                finalizados++;
            }
        }
        return finalizados;
    }

    @EventListener
    public void cerrarIntentosDeSesion(SesionCerrada evento) {
        for (UUID id : intentos.listarActivosDeSesion(evento.idSesion())) {
            cerrarPorConvocatoria(id, evento.instante());
        }
    }

    @EventListener
    public void cerrarIntentoRetirado(ConvocatoriaModificada evento) {
        if (evento.tipoCambio() == ConvocatoriaModificada.TipoCambio.RETIRO) {
            intentos.buscarActivo(evento.idSesion(), evento.idEstudiante())
                    .ifPresent(intento -> cerrarPorConvocatoria(intento.id(), evento.instante()));
        }
    }

    private void cerrarPorConvocatoria(UUID id, Instant ahora) {
        Intento intento = intentos.bloquearPorId(id).orElseThrow(() -> new NoEncontrado("Intento no encontrado"));
        if (intento.estado() == EstadoIntento.EN_PROGRESO) {
            Intento cerrado = intento.estaVencido(ahora) ? intento.vencer(ahora) : intento.finalizar(ahora);
            intentos.actualizar(cerrado);
            publicarFinalizacion(cerrado);
        }
    }

    private void publicarFinalizacion(Intento intento) {
        eventos.publicar(new IntentoFinalizado(UUID.nameUUIDFromBytes(("finalizacion:" + intento.id())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8)), reloj.instant(), intento.idEstudiante(), intento.id()));
    }

    private Intento bloquearPropietario(UUID idEstudiante, UUID idIntento) {
        Intento lectura = exigirPropietario(idEstudiante, idIntento);
        autorizacion.exigirParticipacionVigente(idEstudiante, lectura.idSesion());
        return intentos.bloquearPorId(idIntento).orElseThrow(() -> new NoEncontrado("Intento no encontrado"));
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
                intento.limite(), intento.fin(), intento.estado().name(), intento.modoNavegacion().name(), intento.preguntas(), intento.preguntaActualId(),
                intento.respuestas());
    }
}
