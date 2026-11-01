package com.psicoapp.participation.api;

import com.psicoapp.participation.application.ServicioGestionParticipacion;
import com.psicoapp.participation.application.ServicioGestionParticipacion.DatosIntento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/participaciones")
public class ControladorParticipacion {
    public record SolicitudInicio(@NotNull UUID idSesion, @NotBlank String codigoAcceso,
            @NotEmpty List<UUID> preguntas) { }
    public record SolicitudRespuesta(@NotNull UUID idPregunta, @NotNull UUID idOpcion) { }

    private final ServicioGestionParticipacion participaciones;

    public ControladorParticipacion(ServicioGestionParticipacion participaciones) {
        this.participaciones = participaciones;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DatosIntento iniciar(Principal principal, @Valid @RequestBody SolicitudInicio solicitud) {
        return participaciones.iniciar(UUID.fromString(principal.getName()), solicitud.idSesion(),
                solicitud.codigoAcceso(), solicitud.preguntas());
    }

    @GetMapping("/{id}")
    public DatosIntento consultar(Principal principal, @PathVariable UUID id) {
        return participaciones.consultar(UUID.fromString(principal.getName()), id);
    }

    @PutMapping("/{id}/respuestas")
    public DatosIntento responder(Principal principal, @PathVariable UUID id,
            @Valid @RequestBody SolicitudRespuesta solicitud) {
        return participaciones.responder(UUID.fromString(principal.getName()), id,
                solicitud.idPregunta(), solicitud.idOpcion());
    }

    @PostMapping("/{id}/avanzar")
    public DatosIntento avanzar(Principal principal, @PathVariable UUID id) {
        return participaciones.avanzar(UUID.fromString(principal.getName()), id);
    }

    @PostMapping("/{id}/retroceder")
    public DatosIntento retroceder(Principal principal, @PathVariable UUID id) {
        return participaciones.retroceder(UUID.fromString(principal.getName()), id);
    }

    @PostMapping("/{id}/reanudar")
    public DatosIntento reanudar(Principal principal, @PathVariable UUID id) {
        return participaciones.reanudar(UUID.fromString(principal.getName()), id);
    }

    @PostMapping("/{id}/finalizar")
    public DatosIntento finalizar(Principal principal, @PathVariable UUID id) {
        return participaciones.finalizar(UUID.fromString(principal.getName()), id);
    }
}
