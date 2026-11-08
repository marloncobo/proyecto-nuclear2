package com.psicoapp.notifications.api;

import com.psicoapp.notifications.application.ServicioNotificaciones;
import com.psicoapp.notifications.application.ServicioNotificaciones.DatosNotificacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificaciones")
public class ControladorNotificaciones {
    public record SolicitudConvocatoria(@NotNull UUID idDestinatario, @NotBlank @Email String correo,
            @NotBlank String asunto, @NotBlank String mensaje, String url, @NotBlank String codigoAcceso) { }

    private final ServicioNotificaciones servicio;

    public ControladorNotificaciones(ServicioNotificaciones servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/convocatorias")
    @ResponseStatus(HttpStatus.CREATED)
    public DatosNotificacion convocatoria(Principal principal, @Valid @RequestBody SolicitudConvocatoria solicitud) {
        return servicio.crearConvocatoria(solicitud.idDestinatario(), solicitud.correo(), solicitud.asunto(),
                solicitud.mensaje(), solicitud.url(), solicitud.codigoAcceso());
    }

    @GetMapping("/estudiantes/{idEstudiante}")
    public List<DatosNotificacion> listar(Principal principal, @PathVariable UUID idEstudiante) {
        return servicio.listar(UUID.fromString(principal.getName()), idEstudiante);
    }
}
