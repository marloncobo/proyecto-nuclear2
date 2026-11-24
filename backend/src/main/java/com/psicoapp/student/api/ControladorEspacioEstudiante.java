package com.psicoapp.student.api;

import com.psicoapp.participation.application.ServicioGestionParticipacion.DatosIntento;
import com.psicoapp.student.application.ServicioEspacioEstudiante;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estudiante")
public class ControladorEspacioEstudiante {
    private final ServicioEspacioEstudiante espacio;

    public ControladorEspacioEstudiante(ServicioEspacioEstudiante espacio) {
        this.espacio = espacio;
    }

    @GetMapping("/actividades")
    public List<ServicioEspacioEstudiante.Actividad> listarActividades(Principal principal) {
        return espacio.listarActividades(id(principal));
    }

    @GetMapping("/actividades/{idSesion}")
    public ServicioEspacioEstudiante.Simulacion consultarActividad(Principal principal,
            @PathVariable UUID idSesion) {
        return espacio.consultarActividad(id(principal), idSesion);
    }

    @PostMapping("/actividades/{idSesion}/iniciar")
    @ResponseStatus(HttpStatus.CREATED)
    public DatosIntento iniciar(Principal principal, @PathVariable UUID idSesion) {
        return espacio.iniciar(id(principal), idSesion);
    }

    @GetMapping("/resultados")
    public List<ServicioEspacioEstudiante.Resultado> listarResultados(Principal principal) {
        return espacio.listarResultados(id(principal));
    }

    private UUID id(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
