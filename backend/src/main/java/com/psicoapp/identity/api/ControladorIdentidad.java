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
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identidad")
public class ControladorIdentidad {
    public record SolicitudEdicionDocente(@NotBlank @Email @Size(max = 180) String correo,
            @NotBlank @Size(max = 120) String nombre) { }
    public record SolicitudEstado(@NotNull Boolean activo) { }

    @GetMapping("/usuarios")
    public List<ServicioUsuarios.DatosUsuarioAdministrado> listarUsuarios(Principal principal,
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String busqueda) {
        return identidades.listarUsuarios(UUID.fromString(principal.getName()), rol, activo, busqueda);
    }

    @GetMapping("/usuarios/{idUsuario}")
    public ServicioUsuarios.DatosUsuarioAdministrado consultarUsuario(Principal principal, @PathVariable UUID idUsuario) {
        return identidades.consultarUsuario(UUID.fromString(principal.getName()), idUsuario);
    }

    @PutMapping("/docentes/{idUsuario}")
    public ServicioUsuarios.DatosUsuarioAdministrado editarDocente(Principal principal, @PathVariable UUID idUsuario,
            @Valid @RequestBody SolicitudEdicionDocente solicitud) {
        return identidades.editarDocente(UUID.fromString(principal.getName()), idUsuario, solicitud.correo(), solicitud.nombre());
    }

    @PutMapping("/usuarios/{idUsuario}/estado")
    public ServicioUsuarios.DatosUsuarioAdministrado cambiarEstado(Principal principal, @PathVariable UUID idUsuario,
            @Valid @RequestBody SolicitudEstado solicitud) {
        return identidades.cambiarEstado(UUID.fromString(principal.getName()), idUsuario, solicitud.activo());
    }

    @GetMapping("/roles")
    public List<Rol> listarRoles(Principal principal) {
        return identidades.listarRoles(UUID.fromString(principal.getName()));
    }

    @GetMapping("/roles/{rol}/creacion-casos")
    public SolicitudPermiso consultarCreacionCasos(Principal principal, @PathVariable Rol rol) {
        return new SolicitudPermiso(identidades.consultarCreacionCasos(UUID.fromString(principal.getName()), rol));
    }

    @GetMapping("/estudiantes")
    public DatosUsuario buscarEstudiante(Principal principal,
            @RequestParam String correo) {
        return identidades.buscarEstudiante(UUID.fromString(principal.getName()), correo);
    }

    @GetMapping("/estudiantes/listado")
    public List<DatosUsuario> listarEstudiantes(Principal principal) {
        return identidades.listarEstudiantes(UUID.fromString(principal.getName()));
    }
    public record SolicitudDocente(@NotBlank @Email @Size(max = 180) String correo,
            @NotBlank @Size(max = 120) String nombre, @NotBlank @Size(min = 8, max = 72) String clave) {
        @Override
        public String toString() {
            return "SolicitudDocente[clave=PROTEGIDA]";
        }
    }
    public record SolicitudClave(@NotBlank @Size(min = 8, max = 72) String clave) {
        @Override public String toString() { return "SolicitudClave[PROTEGIDA]"; }
    }

    @PutMapping("/estudiantes/{id}/clave-inicial")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void asignarClaveInicial(Principal principal, @PathVariable UUID id,
            @Valid @RequestBody SolicitudClave solicitud) {
        identidades.asignarClaveInicial(UUID.fromString(principal.getName()), id, solicitud.clave());
    }

    @PutMapping("/clave-inicial")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarClaveInicial(Principal principal, @Valid @RequestBody SolicitudClave solicitud) {
        identidades.cambiarClaveInicial(UUID.fromString(principal.getName()), solicitud.clave());
    }
    public record SolicitudRol(@NotNull Rol rol) { }
    public record SolicitudPermiso(@NotNull Boolean habilitado) { }

    private final ServicioUsuarios identidades;

    public ControladorIdentidad(ServicioUsuarios identidades) {
        this.identidades = identidades;
    }

    @GetMapping("/yo")
    public DatosUsuario yo(Principal principal) {
        return identidades.consultarIdentidad(UUID.fromString(principal.getName()));
    }

    @PostMapping("/docentes")
    @ResponseStatus(HttpStatus.CREATED)
    public DatosUsuario registrarDocente(Principal principal, @Valid @RequestBody SolicitudDocente solicitud) {
        return identidades.registrarDocente(UUID.fromString(principal.getName()), solicitud.correo(), solicitud.nombre(), solicitud.clave());
    }

    @PostMapping("/estudiantes")
    @ResponseStatus(HttpStatus.CREATED)
    public DatosUsuario registrarEstudiante(Principal principal, @Valid @RequestBody SolicitudDocente solicitud) {
        return identidades.registrarEstudiante(UUID.fromString(principal.getName()), solicitud.correo(), solicitud.nombre(), solicitud.clave());
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
