package com.psicoapp.participation.application;

import com.psicoapp.shared.events.*;
import com.psicoapp.sessions.application.AutorizacionConvocatorias;
import com.psicoapp.sessions.application.ConsultaSesiones;
import com.psicoapp.sessions.application.ConsultaSesionesDocente;
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
import org.springframework.transaction.annotation.Isolation;
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
    private final ConsultaSesionesDocente sesionesDocente;
    private final Clock reloj;

    public ServicioGestionParticipacion(RepositorioIntentos intentos, AutorizacionConvocatorias autorizacion,
            ConsultaSesiones sesiones, ConsultaSesionesDocente sesionesDocente, Clock reloj, NotificacionSujeto eventos) {
        this.eventos = eventos;
        this.intentos = intentos;
        this.autorizacion = autorizacion;
        this.sesiones = sesiones;
        this.sesionesDocente = sesionesDocente;
        this.reloj = reloj;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DatosIntento iniciar(UUID idEstudiante, UUID idSesion) {
        autorizacion.exigirAcceso(idEstudiante, idSesion);
        ConsultaSesiones.SesionEnCurso sesion = sesiones.exigirEnCurso(idSesion);
        intentos.bloquearEstudiante(idEstudiante);
        if (intentos.existeActivo(idEstudiante)) {
            throw new Conflicto("El estudiante ya tiene un intento en progreso");
        }
        var ultimo = intentos.buscarUltimo(idSesion, idEstudiante);
        if (ultimo.isPresent() && !intentos.retirarPermisoReintento(ultimo.get().id())) {
            throw new Conflicto("Se requiere autorizacion del docente para un reintento");
        }
        Instant ahora = reloj.instant();
        if (!ahora.isBefore(sesion.finPrevisto())) throw new Conflicto("La sesion ya alcanzo su cierre programado");
        Intento intento = Intento.iniciar(UUID.randomUUID(), idSesion, idEstudiante, ahora,
                sesion.duracionMaximaMin(), sesion.finPrevisto(),
                com.psicoapp.sessions.domain.ModoNavegacion.valueOf(sesion.modoNavegacion()),
                intentos.preguntasDeSesion(idSesion));
        intentos.guardar(intento);
        return representar(intento);
    }

    public DatosIntento iniciar(UUID idEstudiante, UUID idSesion, String codigoIgnorado) {
        return iniciar(idEstudiante, idSesion);
    }

    public DatosIntento entrar(UUID estudiante, UUID sesion, String codigo, boolean ventanaAbierta) {
        var activo = intentos.buscarActivo(sesion, estudiante);
        if (activo.isPresent()) return representar(activo.get());

        var ultimo = intentos.buscarUltimo(sesion, estudiante);
        if (ultimo.isPresent() && (!ventanaAbierta || !intentos.tienePermisoReintento(ultimo.get().id()))) {
            return representar(ultimo.get());
        }
        return iniciar(estudiante, sesion);
    }

    @Transactional(readOnly = true)
    public DatosIntento consultarParaSesion(UUID estudiante, UUID sesion) {
        return intentos.buscarActivo(sesion, estudiante).or(() -> intentos.buscarUltimo(sesion, estudiante))
                .map(this::representar).orElse(null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void autorizarReintento(UUID docente, UUID sesion, UUID estudiante) {
        sesionesDocente.exigirResponsable(docente, sesion);
        autorizacion.exigirParticipacionVigente(estudiante, sesion);
        var enCurso = sesiones.exigirEnCurso(sesion);
        Instant ahora = reloj.instant();
        if (!ahora.isBefore(enCurso.finPrevisto())) {
            throw new Conflicto("La ventana de ingreso ya no admite reintentos");
        }
        intentos.bloquearEstudiante(estudiante);
        if (intentos.existeActivo(estudiante)) {
            throw new Conflicto("El estudiante ya tiene un intento en progreso");
        }
        Intento anterior = intentos.buscarUltimo(sesion, estudiante)
                .orElseThrow(() -> new Conflicto("El estudiante aun no ha realizado un intento"));
        if (!intentos.tienePermisoReintento(anterior.id())) {
            intentos.registrarPermisoReintento(anterior.id(), docente, ahora);
        }
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
            intentos.buscarUltimo(evento.idSesion(), evento.idEstudiante())
                    .ifPresent(intento -> intentos.retirarPermisoReintento(intento.id()));
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
