package com.psicoapp.identity.api;

import com.psicoapp.identity.application.DirectorioIdentidad.DatosUsuario;
import com.psicoapp.identity.application.ServicioUsuarios;
import com.psicoapp.identity.domain.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identidad")
public class ControladorIdentidad {
    public record SolicitudDocente(@NotBlank @Email @Size(max = 180) String correo,
            @NotBlank @Size(max = 120) String nombre, @NotBlank @Size(min = 8, max = 72) String clave) {
        @Override
        public String toString() {
            return "SolicitudDocente[clave=PROTEGIDA]";
        }
    }
    public record SolicitudEstudiante(@NotBlank @Email @Size(max = 180) String correo,
            @NotBlank @Size(max = 120) String nombre) { }
    public record SolicitudRol(@NotNull Rol rol) { }
    public record SolicitudPermiso(@NotNull Boolean habilitado) { }
    public record RespuestaCsrf(String nombreCabecera, String token) {
        @Override
        public String toString() {
            return "RespuestaCsrf[token=PROTEGIDA]";
        }
    }

    private final ServicioUsuarios identidades;

    public ControladorIdentidad(ServicioUsuarios identidades) {
        this.identidades = identidades;
    }

    @GetMapping("/yo")
    public DatosUsuario yo(Principal principal) {
        return identidades.consultarIdentidad(UUID.fromString(principal.getName()));
    }

    @GetMapping("/csrf")
    public RespuestaCsrf csrf(CsrfToken token) {
        return new RespuestaCsrf(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/docentes")
    @ResponseStatus(HttpStatus.CREATED)
    public DatosUsuario registrarDocente(Principal principal, @Valid @RequestBody SolicitudDocente solicitud) {
        return identidades.registrarDocente(UUID.fromString(principal.getName()), solicitud.correo(), solicitud.nombre(), solicitud.clave());
    }

    @PostMapping("/estudiantes")
    @ResponseStatus(HttpStatus.CREATED)
    public DatosUsuario registrarEstudiante(Principal principal, @Valid @RequestBody SolicitudEstudiante solicitud) {
        return identidades.registrarEstudiante(UUID.fromString(principal.getName()), solicitud.correo(), solicitud.nombre());
    }

    @PutMapping("/usuarios/{idUsuario}/rol")
    public DatosUsuario cambiarRol(Principal principal, @PathVariable UUID idUsuario, @Valid @RequestBody SolicitudRol solicitud) {
        return identidades.cambiarRol(UUID.fromString(principal.getName()), idUsuario, solicitud.rol());
    }

    @PutMapping("/roles/{rol}/creacion-casos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void configurarCreacionCasos(Principal principal, @PathVariable Rol rol, @Valid @RequestBody SolicitudPermiso solicitud) {
        identidades.configurarCreacionCasos(UUID.fromString(principal.getName()), rol, solicitud.habilitado());
    }
}
