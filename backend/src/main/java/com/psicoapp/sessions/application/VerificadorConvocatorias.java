package com.psicoapp.sessions.application;

import com.psicoapp.identity.application.VerificadorAccesoEstudiante;
import com.psicoapp.sessions.domain.Convocatoria;
import com.psicoapp.sessions.domain.Sesion;
import com.psicoapp.sessions.domain.TipoEstadoSesion;
import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.time.Duration;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Verifica la convocatoria de la cuenta autenticada para la sesion concreta. */
@Service
@Transactional(readOnly = true)
public class VerificadorConvocatorias implements VerificadorAccesoEstudiante, AutorizacionConvocatorias {
    private final RepositorioConvocatorias convocatorias;
    private final RepositorioSesiones sesiones;
    private final Clock reloj;
    private final DirectorioIdentidad identidades;
    public VerificadorConvocatorias(RepositorioConvocatorias convocatorias, RepositorioSesiones sesiones, Clock reloj,
            DirectorioIdentidad identidades) {
        this.convocatorias = convocatorias;
        this.sesiones = sesiones;
        this.reloj = reloj;
        this.identidades = identidades;
    }

    @Override
    public boolean verificar(UUID estudiante, String codigo) {
        if (codigo == null || !codigo.matches("[0-9a-f]{32}")) return false;
        return verificarResumen(estudiante, Convocatoria.resumir(codigo));
    }

    @Override
    public boolean verificarResumen(UUID estudiante, String resumen) {
        if (resumen == null || !resumen.matches("[0-9a-f]{64}")) return false;
        return convocatorias.buscarAutorizada(estudiante, resumen)
                .flatMap(c -> sesiones.buscarPorId(c.idSesion()))
                .isPresent();
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void exigirAcceso(UUID estudiante, UUID idSesion) {
        var sesion = exigirConvocatoria(estudiante, idSesion);
        if (!permiteIngreso(sesion)) {
            throw new AccesoDenegado("El estado o la ventana de ingreso no permite iniciar participacion");
        }
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void exigirAcceso(UUID estudiante, UUID idSesion, String codigoIgnorado) {
        exigirAcceso(estudiante, idSesion);
    }

    public boolean permiteIngreso(Sesion sesion) {
        var ahora = reloj.instant();
        var limite = sesion.inicioPrevisto().plus(Duration.between(sesion.inicioPrevisto(), sesion.finPrevisto()).dividedBy(2));
        return sesion.estado().codigo() == TipoEstadoSesion.EN_CURSO && !sesion.estaVencida(ahora)
                && !ahora.isBefore(sesion.inicioPrevisto()) && !ahora.isAfter(limite);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void exigirParticipacionVigente(UUID estudiante, UUID idSesion) {
        identidades.exigirEstudiante(estudiante);
        var sesion = sesiones.bloquearPorId(idSesion).orElseThrow(() -> new NoEncontrado("Sesion no encontrada"));
        if (sesion.estado().codigo() != TipoEstadoSesion.EN_CURSO || sesion.estaVencida(reloj.instant())) {
            throw new AccesoDenegado("La sesion ya no permite continuar la participacion");
        }
        if (convocatorias.listarPorSesion(idSesion).stream()
                .noneMatch(c -> c.idEstudiante().equals(estudiante) && c.autorizada())) {
            throw new AccesoDenegado("La convocatoria ya no autoriza esta participacion");
        }
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Sesion exigirConvocatoria(UUID estudiante, UUID idSesion, String codigo) {
        return exigirConvocatoria(estudiante, idSesion);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Sesion exigirConvocatoria(UUID estudiante, UUID idSesion) {
        identidades.exigirEstudiante(estudiante);
        var sesion = sesiones.bloquearPorId(idSesion).orElseThrow(() -> new NoEncontrado("Sesion no encontrada"));
        if (convocatorias.buscarAutorizada(estudiante, idSesion).isEmpty()) {
            throw new AccesoDenegado("No esta autorizado para esta sesion");
        }
        return sesion;
    }
}
