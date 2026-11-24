package com.psicoapp.student.application;

import com.psicoapp.cases.application.RepositorioCasos;
import com.psicoapp.cases.domain.Caso;
import com.psicoapp.evaluation.application.RepositorioEvaluaciones;
import com.psicoapp.evaluation.application.RepositorioValoraciones;
import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.participation.application.RepositorioIntentos;
import com.psicoapp.participation.application.ServicioGestionParticipacion;
import com.psicoapp.participation.application.ServicioGestionParticipacion.DatosIntento;
import com.psicoapp.participation.domain.EstadoIntento;
import com.psicoapp.participation.domain.Intento;
import com.psicoapp.sessions.application.RepositorioConvocatorias;
import com.psicoapp.sessions.application.RepositorioSesiones;
import com.psicoapp.sessions.domain.Sesion;
import com.psicoapp.sessions.domain.TipoEstadoSesion;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServicioEspacioEstudiante {
    public record Actividad(UUID idSesion, UUID idCaso, String nombreCaso, String contexto,
            Instant inicioPrevisto, Instant finPrevisto, int duracionMaximaMin, String modoNavegacion,
            String estadoSesion, String lugar, String mensaje, UUID idIntento, String estadoIntento,
            String accion) { }

    public record OpcionVisible(UUID id, String texto) { }
    public record PreguntaVisible(UUID id, String escenario, String contexto, String enunciado,
            List<OpcionVisible> opciones) { }
    public record Simulacion(Actividad actividad, List<PreguntaVisible> preguntas, DatosIntento intento) { }

    public record RespuestaResultado(UUID idPregunta, String enunciado, UUID idOpcion,
            String respuesta, boolean contestada, boolean correcta, BigDecimal puntos,
            String retroalimentacion, List<String> respuestasCorrectas, List<String> explicaciones) { }
    public record Valoracion(UUID id, String mensaje, Instant fecha) { }
    public record Resultado(UUID idIntento, UUID idSesion, String nombreCaso, String estadoIntento,
            Instant inicio, Instant fin, BigDecimal calificacion, Instant fechaEvaluacion,
            List<RespuestaResultado> respuestas, List<Valoracion> valoraciones) { }

    private final DirectorioIdentidad identidades;
    private final RepositorioConvocatorias convocatorias;
    private final RepositorioSesiones sesiones;
    private final RepositorioCasos casos;
    private final RepositorioIntentos intentos;
    private final RepositorioEvaluaciones evaluaciones;
    private final RepositorioValoraciones valoraciones;
    private final ServicioGestionParticipacion participaciones;
    private final Clock reloj;

    public ServicioEspacioEstudiante(DirectorioIdentidad identidades, RepositorioConvocatorias convocatorias,
            RepositorioSesiones sesiones, RepositorioCasos casos, RepositorioIntentos intentos,
            RepositorioEvaluaciones evaluaciones, RepositorioValoraciones valoraciones,
            ServicioGestionParticipacion participaciones, Clock reloj) {
        this.identidades = identidades;
        this.convocatorias = convocatorias;
        this.sesiones = sesiones;
        this.casos = casos;
        this.intentos = intentos;
        this.evaluaciones = evaluaciones;
        this.valoraciones = valoraciones;
        this.participaciones = participaciones;
        this.reloj = reloj;
    }

    public List<Actividad> listarActividades(UUID estudiante) {
        identidades.exigirEstudiante(estudiante);
        return convocatorias.listarPorEstudiante(estudiante).stream()
                .filter(convocatoria -> convocatoria.autorizada())
                .map(convocatoria -> sesiones.buscarPorId(convocatoria.idSesion()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(sesion -> actividad(estudiante, sesion))
                .sorted(java.util.Comparator.comparing(Actividad::inicioPrevisto).reversed())
                .toList();
    }

    public Simulacion consultarActividad(UUID estudiante, UUID idSesion) {
        identidades.exigirEstudiante(estudiante);
        exigirConvocatoria(estudiante, idSesion);
        Sesion sesion = sesiones.buscarPorId(idSesion).orElseThrow(() -> new NoEncontrado("Sesion no encontrada"));
        Caso caso = casos.buscarPorId(sesion.idCaso()).orElseThrow(() -> new NoEncontrado("Caso no encontrado"));
        List<PreguntaVisible> preguntas = caso.escenarios().stream().flatMap(escenario ->
                escenario.preguntas().stream().map(pregunta -> new PreguntaVisible(pregunta.id(),
                        escenario.titulo(), escenario.contextoSituacional(), pregunta.enunciado(),
                        pregunta.opciones().stream().map(opcion -> new OpcionVisible(opcion.id(), opcion.texto())).toList())))
                .toList();
        DatosIntento intento = participaciones.consultarParaSesion(estudiante, idSesion);
        return new Simulacion(actividad(estudiante, sesion), preguntas, intento);
    }

    @Transactional
    public DatosIntento iniciar(UUID estudiante, UUID idSesion) {
        return participaciones.iniciar(estudiante, idSesion);
    }

    public List<Resultado> listarResultados(UUID estudiante) {
        identidades.exigirEstudiante(estudiante);
        return intentos.listarPorEstudiante(estudiante).stream()
                .filter(intento -> intento.estado() != EstadoIntento.EN_PROGRESO)
                .map(this::resultado)
                .toList();
    }

    private Resultado resultado(Intento intento) {
        Sesion sesion = sesiones.buscarPorId(intento.idSesion())
                .orElseThrow(() -> new NoEncontrado("Sesion no encontrada"));
        Caso caso = casos.buscarPorId(sesion.idCaso()).orElseThrow(() -> new NoEncontrado("Caso no encontrado"));
        var preguntas = caso.escenarios().stream().flatMap(escenario -> escenario.preguntas().stream())
                .collect(Collectors.toMap(pregunta -> pregunta.id(), Function.identity()));
        var evaluacion = evaluaciones.buscarPorIntento(intento.id()).orElse(null);
        List<RespuestaResultado> respuestas = evaluacion == null ? List.of() :
                evaluacion.resultado().detalles().stream().map(detalle -> {
                    var pregunta = preguntas.get(detalle.idPregunta());
                    var seleccionada = pregunta == null ? null : pregunta.opciones().stream()
                            .filter(opcion -> opcion.id().equals(detalle.idOpcion())).findFirst().orElse(null);
                    return new RespuestaResultado(detalle.idPregunta(), pregunta == null ? "Pregunta" : pregunta.enunciado(),
                            detalle.idOpcion(), seleccionada == null ? null : seleccionada.texto(), detalle.contestada(),
                            detalle.correcta(), detalle.puntos(), detalle.retroalimentacion(),
                            pregunta == null ? List.of() : pregunta.opciones().stream().filter(opcion -> opcion.esCorrecta())
                                    .map(opcion -> opcion.texto()).toList(),
                            pregunta == null ? List.of() : pregunta.opciones().stream().filter(opcion -> opcion.esCorrecta())
                                    .map(opcion -> opcion.justificacion()).toList());
                }).toList();
        List<Valoracion> datosValoraciones = valoraciones.listar(intento.id()).stream()
                .map(valoracion -> new Valoracion(valoracion.id(), valoracion.mensaje(), valoracion.fecha())).toList();
        return new Resultado(intento.id(), intento.idSesion(), caso.nombre(), intento.estado().name(),
                intento.inicio(), intento.fin(), evaluacion == null ? null : evaluacion.resultado().calificacion(),
                evaluacion == null ? null : evaluacion.fecha(), respuestas, datosValoraciones);
    }

    private Actividad actividad(UUID estudiante, Sesion sesion) {
        Intento intento = intentos.buscarActivo(sesion.id(), estudiante)
                .or(() -> intentos.buscarUltimo(sesion.id(), estudiante)).orElse(null);
        String accion = accion(sesion, intento);
        Caso caso = casos.buscarPorId(sesion.idCaso()).orElseThrow(() -> new NoEncontrado("Caso no encontrado"));
        return new Actividad(sesion.id(), sesion.idCaso(), caso.nombre(), caso.contexto(), sesion.inicioPrevisto(),
                sesion.finPrevisto(), sesion.duracionMaximaMin(), sesion.modoNavegacion().name(),
                sesion.estado().codigo().name(), sesion.lugar(), sesion.mensaje(),
                intento == null ? null : intento.id(), intento == null ? null : intento.estado().name(), accion);
    }

    private String accion(Sesion sesion, Intento intento) {
        if (intento != null && intento.estado() == EstadoIntento.EN_PROGRESO) return "CONTINUAR";
        boolean ventana = permiteIngreso(sesion);
        if (intento == null) return ventana ? "INICIAR" : "ESPERAR";
        if (ventana && intentos.tienePermisoReintento(intento.id())) return "INICIAR";
        return "RESULTADO";
    }

    private boolean permiteIngreso(Sesion sesion) {
        Instant ahora = reloj.instant();
        Instant limite = sesion.inicioPrevisto().plus(Duration.between(sesion.inicioPrevisto(), sesion.finPrevisto()).dividedBy(2));
        return sesion.estado().codigo() == TipoEstadoSesion.EN_CURSO && !sesion.estaVencida(ahora)
                && !ahora.isBefore(sesion.inicioPrevisto()) && !ahora.isAfter(limite);
    }

    private void exigirConvocatoria(UUID estudiante, UUID sesion) {
        if (convocatorias.buscarAutorizada(estudiante, sesion).isEmpty()) {
            throw new AccesoDenegado("No esta autorizado para esta sesion");
        }
    }
}
