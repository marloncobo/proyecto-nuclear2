package com.psicoapp.sessions.api;

import com.psicoapp.sessions.application.ServicioGestionSesiones;
import com.psicoapp.sessions.application.ServicioGestionSesiones.DatosProgramacion;
import com.psicoapp.sessions.application.ServicioGestionSesiones.DatosSesion;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sesiones")
public class ControladorSesiones {
    private final ServicioGestionSesiones sesiones;

    public ControladorSesiones(ServicioGestionSesiones sesiones) {
        this.sesiones = sesiones;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DatosSesion programar(Principal principal, @RequestBody DatosProgramacion solicitud) {
        return sesiones.programar(UUID.fromString(principal.getName()), solicitud);
    }

    @GetMapping("/{id}")
    public DatosSesion consultar(Principal principal, @PathVariable UUID id) {
        return sesiones.consultar(UUID.fromString(principal.getName()), id);
    }

    @PostMapping("/{id}/iniciar")
    public DatosSesion iniciar(Principal principal, @PathVariable UUID id) {
        return sesiones.iniciar(UUID.fromString(principal.getName()), id);
    }

    @PostMapping("/{id}/finalizar")
    public DatosSesion finalizar(Principal principal, @PathVariable UUID id) {
        return sesiones.finalizar(UUID.fromString(principal.getName()), id);
    }
}
