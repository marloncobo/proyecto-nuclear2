package com.psicoapp.academic.api;

import com.psicoapp.academic.application.FachadaAdministracion;
import com.psicoapp.academic.application.FachadaAdministracion.DatosEspacioAcademico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academico/espacios")
public class ControladorEspaciosAcademicos {
    public record SolicitudEspacio(@NotBlank @Size(max = 150) String nombre, @NotBlank @Size(max = 30) String periodo) { }

    private final FachadaAdministracion academico;

    public ControladorEspaciosAcademicos(FachadaAdministracion academico) {
        this.academico = academico;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DatosEspacioAcademico crearEspacio(Principal principal, @Valid @RequestBody SolicitudEspacio solicitud) {
        return academico.crearEspacio(UUID.fromString(principal.getName()), solicitud.nombre(), solicitud.periodo());
    }

    @GetMapping("/{id}")
    public DatosEspacioAcademico consultarEspacio(Principal principal, @PathVariable UUID id) {
        return academico.consultarEspacio(UUID.fromString(principal.getName()), id);
    }

    @GetMapping
    public List<DatosEspacioAcademico> listarEspacios(Principal principal) {
        return academico.listarEspacios(UUID.fromString(principal.getName()));
    }
}
