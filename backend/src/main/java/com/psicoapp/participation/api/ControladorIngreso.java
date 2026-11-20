package com.psicoapp.participation.api;

import com.psicoapp.cases.application.RepositorioCasos;
import com.psicoapp.participation.application.ServicioGestionParticipacion;
import com.psicoapp.participation.application.ServicioGestionParticipacion.DatosIntento;
import com.psicoapp.sessions.application.VerificadorConvocatorias;
import com.psicoapp.shared.exceptions.NoEncontrado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/participaciones/ingreso")
public class ControladorIngreso {
    public record Solicitud(@NotNull UUID idSesion, @NotBlank String codigoAcceso) { }
    public record OpcionVisible(UUID id, String texto) { }
    public record PreguntaVisible(UUID id, String escenario, String contexto, String enunciado,
            List<OpcionVisible> opciones) { }
    public record Simulacion(String nombre, List<PreguntaVisible> preguntas, DatosIntento intento) { }

    private final VerificadorConvocatorias acceso;
    private final RepositorioCasos casos;
    private final ServicioGestionParticipacion participaciones;

    public ControladorIngreso(VerificadorConvocatorias acceso, RepositorioCasos casos,
            ServicioGestionParticipacion participaciones) {
        this.acceso = acceso;
        this.casos = casos;
        this.participaciones = participaciones;
    }

    @PostMapping
    @Transactional
    public Simulacion ingresar(Principal principal, @Valid @RequestBody Solicitud solicitud) {
        UUID estudiante = UUID.fromString(principal.getName());
        var sesion = acceso.exigirConvocatoria(estudiante, solicitud.idSesion(), solicitud.codigoAcceso());
        var caso = casos.buscarPorId(sesion.idCaso()).orElseThrow(() -> new NoEncontrado("Caso no encontrado"));
        var preguntas = caso.escenarios().stream().flatMap(escenario -> escenario.preguntas().stream()
                .map(pregunta -> new PreguntaVisible(pregunta.id(), escenario.titulo(),
                        escenario.contextoSituacional(), pregunta.enunciado(), pregunta.opciones().stream()
                        .map(opcion -> new OpcionVisible(opcion.id(), opcion.texto())).toList()))).toList();
        var intento = participaciones.consultarUltimo(estudiante, solicitud.idSesion()).orElseGet(() ->
                participaciones.iniciar(estudiante, solicitud.idSesion(), solicitud.codigoAcceso(),
                        preguntas.stream().map(PreguntaVisible::id).toList()));
        return new Simulacion(caso.nombre(), preguntas, intento);
    }
}
