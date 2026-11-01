package com.psicoapp.evaluation.api;

import com.psicoapp.evaluation.application.ServicioEvaluacion;
import com.psicoapp.evaluation.application.ServicioEvaluacion.DatosEvaluacion;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluaciones")
public class ControladorEvaluacion {
    private final ServicioEvaluacion evaluacion;

    public ControladorEvaluacion(ServicioEvaluacion evaluacion) {
        this.evaluacion = evaluacion;
    }

    @PostMapping("/{idIntento}")
    @ResponseStatus(HttpStatus.CREATED)
    public DatosEvaluacion evaluar(Principal principal, @PathVariable UUID idIntento) {
        return evaluacion.evaluar(UUID.fromString(principal.getName()), idIntento);
    }

    @GetMapping("/{idIntento}")
    public DatosEvaluacion consultar(Principal principal, @PathVariable UUID idIntento) {
        return evaluacion.consultar(UUID.fromString(principal.getName()), idIntento);
    }
}
