package com.psicoapp.sessions.application;

import com.psicoapp.academic.application.FachadaAdministracion;
import com.psicoapp.cases.application.CatalogoCasos;
import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.sessions.domain.*;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.Conflicto;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioGestionSesiones implements ConsultaSesiones, ConsultaSesionesDocente {
    public record DatosProgramacion(UUID idCaso, UUID idEspacio, Instant inicioPrevisto, Instant finPrevisto,
            int duracionMaximaMin, ModoNavegacion modoNavegacion, String lugar, String mensaje,
            List<UUID> estudiantes) { }

    public record DatosConvocatoria(UUID id, UUID idEstudiante, String codigoAcceso) { }

    public record DatosSesion(UUID id, UUID idCaso, UUID idEspacio, UUID idDocente, Instant inicioPrevisto,
            Instant finPrevisto, int duracionMaximaMin, String modoNavegacion, String lugar, String mensaje, String estado,
            List<DatosConvocatoria> convocatorias) { }

    private final RepositorioSesiones sesiones;
    private final DirectorioIdentidad identidades;
    private final CatalogoCasos casos;
    private final FachadaAdministracion academico;
    private final Clock reloj;
    private final RepositorioConvocatorias convocatorias;

    public ServicioGestionSesiones(RepositorioSesiones sesiones, DirectorioIdentidad identidades,
            CatalogoCasos casos, FachadaAdministracion academico, Clock reloj, RepositorioConvocatorias convocatorias) {
        this.sesiones = sesiones;
        this.identidades = identidades;
        this.casos = casos;
        this.academico = academico;
        this.reloj = reloj;
        this.convocatorias = convocatorias;
    }

    public DatosSesion programar(UUID idDocente, DatosProgramacion datos) {
        identidades.exigirDocente(idDocente);
        if (datos == null || datos.idCaso() == null || datos.idEspacio() == null) {
            throw new IllegalArgumentException("Los datos, caso y espacio son obligatorios");
        }
        if (datos.estudiantes() == null || datos.estudiantes().isEmpty()
                || datos.estudiantes().stream().anyMatch(java.util.Objects::isNull)
                || new HashSet<>(datos.estudiantes()).size() != datos.estudiantes().size()) {
            throw new IllegalArgumentException("Debe convocar al menos un estudiante, sin nulos ni duplicados");
        }
        List<UUID> estudiantes = List.copyOf(datos.estudiantes());
        estudiantes.forEach(identidades::exigirEstudiante);
        var caso = casos.exigirPublicado(idDocente, datos.idCaso());
        academico.consultarEspacio(idDocente, datos.idEspacio());
        Sesion sesion = Sesion.programar(UUID.randomUUID(), caso.id(), datos.idEspacio(), idDocente,
                datos.inicioPrevisto(), datos.finPrevisto(), datos.duracionMaximaMin(), caso.tiempoEstimadoMin(),
                datos.modoNavegacion(), datos.lugar(), datos.mensaje());
        sesiones.guardar(sesion);
        List<DatosConvocatoria> credenciales = new ArrayList<>();
        for (UUID estudiante : estudiantes) {
            String codigo = UUID.randomUUID().toString().replace("-", "");
            Convocatoria convocatoria = new Convocatoria(UUID.randomUUID(), sesion.id(), estudiante, Convocatoria.resumir(codigo), true);
            convocatorias.guardar(convocatoria);
            credenciales.add(new DatosConvocatoria(convocatoria.id(), estudiante, codigo));
        }
        return representar(sesion, List.copyOf(credenciales));
    }

    @Override
    @Transactional(readOnly = true)
    public void exigirResponsable(UUID idDocente, UUID idSesion) {
        identidades.exigirDocente(idDocente);
        Sesion sesion = sesiones.buscarPorId(idSesion).orElseThrow(() -> new NoEncontrado("Sesion no encontrada"));
        exigirResponsable(idDocente, sesion);
    }

    @Transactional(readOnly = true)
    public DatosSesion consultar(UUID idDocente, UUID idSesion) {
        identidades.exigirDocente(idDocente);
        Sesion sesion = sesiones.buscarPorId(idSesion).orElseThrow(() -> new NoEncontrado("Sesion no encontrada"));
        exigirResponsable(idDocente, sesion);
        return representar(sesion);
    }

    public DatosSesion iniciar(UUID idDocente, UUID idSesion) {
        identidades.exigirDocente(idDocente);
        Sesion sesion = bloquear(idSesion);
        exigirResponsable(idDocente, sesion);
        Sesion iniciada = transicion(() -> sesion.iniciar(reloj.instant()));
        sesiones.actualizarEstado(iniciada);
        return representar(iniciada);
    }

    public DatosSesion finalizar(UUID idDocente, UUID idSesion) {
        identidades.exigirDocente(idDocente);
        Sesion sesion = bloquear(idSesion);
        exigirResponsable(idDocente, sesion);
        Sesion cerrada = transicion(() -> sesion.finalizar(MotivoCierre.DOCENTE, reloj.instant()));
        if (cerrada != sesion) sesiones.actualizarEstado(cerrada);
        return representar(cerrada);
    }

    /** El disparador aporta un unico instante del reloj para todo el barrido. */
    public int cerrarVencidas(Instant ahora) {
        if (ahora == null) throw new IllegalArgumentException("El instante es obligatorio");
        int cerradas = 0;
        for (UUID id : sesiones.buscarVencidas(ahora.minus(Duration.ofDays(1)))) {
            Sesion sesion = bloquear(id);
            // Revalidar bajo bloqueo: otro proceso pudo finalizar desde la consulta inicial.
            if (sesion.estado().codigo() != TipoEstadoSesion.FINALIZADO && sesion.estaVencida(ahora)) {
                sesiones.actualizarEstado(sesion.finalizar(MotivoCierre.AUTOMATICO, ahora));
                cerradas++;
            }
        }
        return cerradas;
    }

    @Override
    public SesionEnCurso exigirEnCurso(UUID idSesion) {
        Sesion sesion = bloquear(idSesion);
        transicion(() -> {
            sesion.validarParticipacion(reloj.instant());
            return sesion;
        });
        return new SesionEnCurso(sesion.id(), sesion.idCaso(), sesion.inicioPrevisto(), sesion.finPrevisto(),
                sesion.duracionMaximaMin(), sesion.modoNavegacion().name());
    }

    private Sesion bloquear(UUID id) {
        return sesiones.bloquearPorId(id).orElseThrow(() -> new NoEncontrado("Sesion no encontrada"));
    }

    private void exigirResponsable(UUID idDocente, Sesion sesion) {
        if (!sesion.idDocente().equals(idDocente)) {
            throw new AccesoDenegado("Solo el docente responsable puede administrar la sesion");
        }
    }

    private Sesion transicion(Supplier<Sesion> operacion) {
        try {
            return operacion.get();
        } catch (TransicionSesionInvalida excepcion) {
            throw new Conflicto(excepcion.getMessage());
        }
    }

    private DatosSesion representar(Sesion sesion) {
        return representar(sesion, convocatorias.listarPorSesion(sesion.id()).stream()
                .map(c -> new DatosConvocatoria(c.id(), c.idEstudiante(), null)).toList());
    }

    private DatosSesion representar(Sesion sesion, List<DatosConvocatoria> datosConvocatorias) {
        return new DatosSesion(sesion.id(), sesion.idCaso(), sesion.idEspacio(), sesion.idDocente(),
                sesion.inicioPrevisto(), sesion.finPrevisto(), sesion.duracionMaximaMin(),
                sesion.modoNavegacion().name(), sesion.lugar(), sesion.mensaje(), sesion.estado().codigo().name(), datosConvocatorias);
    }
}
