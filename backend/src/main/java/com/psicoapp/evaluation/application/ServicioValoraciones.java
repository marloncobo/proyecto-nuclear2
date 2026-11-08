package com.psicoapp.evaluation.application;

import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.sessions.application.AutorizacionDocenteSesiones;
import com.psicoapp.shared.events.*;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioValoraciones implements GestionValoraciones {
    private final ServicioEvaluacion evaluaciones;
    private final AutorizacionDocenteSesiones sesiones;
    private final DirectorioIdentidad identidades;
    private final RepositorioValoraciones valoraciones;
    private final NotificacionSujeto eventos;
    private final Clock reloj;
    public ServicioValoraciones(ServicioEvaluacion evaluaciones, AutorizacionDocenteSesiones sesiones,
            DirectorioIdentidad identidades, RepositorioValoraciones valoraciones, NotificacionSujeto eventos, Clock reloj) {
        this.evaluaciones = evaluaciones; this.sesiones = sesiones; this.identidades = identidades;
        this.valoraciones = valoraciones; this.eventos = eventos; this.reloj = reloj;
    }
    @Override
    public ValoracionPublicada registrar(UUID docente, UUID intento, String mensaje) {
        identidades.exigirDocente(docente);
        var referencia = evaluaciones.exigirFinalizado(intento);
        sesiones.exigirResponsable(docente, referencia.idSesion());
        if (mensaje == null || mensaje.isBlank() || mensaje.length() > 4000)
            throw new IllegalArgumentException("La valoracion debe contener entre 1 y 4000 caracteres");
        var valoracion = new RepositorioValoraciones.Valoracion(UUID.randomUUID(), intento, docente, mensaje.strip(), reloj.instant());
        valoraciones.guardar(valoracion);
        eventos.publicar(new RetroalimentacionAgregada(UUID.randomUUID(), reloj.instant(), referencia.idEstudiante(),
                referencia.idSesion(), intento, valoracion.id()));
        return representar(valoracion);
    }
    @Override
    @Transactional(readOnly = true)
    public List<ValoracionPublicada> consultar(UUID actor, UUID intento) {
        var referencia = evaluaciones.exigirFinalizado(intento);
        var identidad = identidades.consultarIdentidad(actor);
        if ("DOCENTE".equals(identidad.rol())) sesiones.exigirResponsable(actor, referencia.idSesion());
        else if (!"ESTUDIANTE".equals(identidad.rol()) || !actor.equals(referencia.idEstudiante()))
            throw new AccesoDenegado("No puede consultar las valoraciones de este intento");
        return valoraciones.listar(intento).stream().map(this::representar).toList();
    }
    private ValoracionPublicada representar(RepositorioValoraciones.Valoracion valoracion) {
        return new ValoracionPublicada(valoracion.id(), valoracion.idIntento(), valoracion.idDocente(),
                valoracion.mensaje(), valoracion.fecha());
    }
}
