package com.psicoapp.participation.api;

import com.psicoapp.participation.application.ServicioGestionParticipacion;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sesiones/{sesion}/reintentos")
public class ControladorReintentos {
    private final ServicioGestionParticipacion participaciones;

    public ControladorReintentos(ServicioGestionParticipacion participaciones) {
        this.participaciones = participaciones;
    }

    @PutMapping("/{estudiante}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void autorizar(Principal principal, @PathVariable UUID sesion, @PathVariable UUID estudiante) {
        participaciones.autorizarReintento(UUID.fromString(principal.getName()), sesion, estudiante);
    }
}
