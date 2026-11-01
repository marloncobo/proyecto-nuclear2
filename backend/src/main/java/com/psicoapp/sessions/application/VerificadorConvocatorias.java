package com.psicoapp.sessions.application;

import com.psicoapp.identity.application.VerificadorAccesoEstudiante;
import com.psicoapp.sessions.domain.Convocatoria;
import com.psicoapp.sessions.domain.TipoEstadoSesion;
import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.time.Duration;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Verifica correo/usuario + codigo. Participation debe autorizar la sesion concreta en su transaccion. */
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
        return convocatorias.buscarAutorizada(estudiante, Convocatoria.resumir(codigo))
                .flatMap(c -> sesiones.buscarPorId(c.idSesion()))
                .filter(s -> s.estado().codigo() == TipoEstadoSesion.EN_CURSO && !s.estaVencida(reloj.instant()))
                .isPresent();
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void exigirAcceso(UUID estudiante, UUID idSesion, String codigo) {
        identidades.exigirEstudiante(estudiante);
        var sesion = sesiones.bloquearPorId(idSesion).orElseThrow(() -> new NoEncontrado("Sesion no encontrada"));
        if (codigo == null || !codigo.matches("[0-9a-f]{32}")) throw new AccesoDenegado("Codigo invalido");
        var convocatoria = convocatorias.buscarAutorizada(estudiante, Convocatoria.resumir(codigo));
        if (convocatoria.isEmpty() || !convocatoria.get().idSesion().equals(idSesion)) {
            throw new AccesoDenegado("No esta autorizado para esta sesion");
        }
        var ahora = reloj.instant();
        var limite = sesion.inicioPrevisto().plus(Duration.between(sesion.inicioPrevisto(), sesion.finPrevisto()).dividedBy(2));
        if (sesion.estado().codigo() != TipoEstadoSesion.EN_CURSO || sesion.estaVencida(ahora)
                || ahora.isBefore(sesion.inicioPrevisto()) || ahora.isAfter(limite)) {
            throw new AccesoDenegado("El estado o la ventana de ingreso no permite iniciar participacion");
        }
    }
}
