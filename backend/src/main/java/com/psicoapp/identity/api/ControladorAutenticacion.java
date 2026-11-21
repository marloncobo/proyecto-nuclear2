package com.psicoapp.identity.api;

import com.psicoapp.identity.application.Autenticacion;
import com.psicoapp.identity.application.Autenticacion.DatosToken;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/identidad/ingreso")
public class ControladorAutenticacion {
    public record SolicitudIngreso(@NotBlank @Email @Size(max = 180) String correo,
            @NotBlank @Size(max = 256) String clave) {
        @Override public String toString() { return "SolicitudIngreso[PROTEGIDA]"; }
    }
    private final Autenticacion autenticacion;

    public ControladorAutenticacion(Autenticacion autenticacion) {
        this.autenticacion = autenticacion;
    }

    @PostMapping
    public ResponseEntity<DatosToken> ingresar(@Valid @RequestBody SolicitudIngreso solicitud) {
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(autenticacion.ingresar(solicitud.correo(), solicitud.clave()));
        } catch (AuthenticationException excepcion) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }
    }
}
