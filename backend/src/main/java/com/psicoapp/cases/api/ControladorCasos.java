package com.psicoapp.cases.api;

import com.psicoapp.cases.application.ServicioCreacionCaso;
import com.psicoapp.cases.application.ServicioCreacionCaso.DatosCaso;
import com.psicoapp.cases.application.ServicioCreacionCaso.DatosCreacionCaso;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/casos")
public class ControladorCasos {
    private final ServicioCreacionCaso casos;

    public ControladorCasos(ServicioCreacionCaso casos) {
        this.casos = casos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DatosCaso crear(Principal principal, @RequestBody DatosCreacionCaso solicitud) {
        return casos.crear(UUID.fromString(principal.getName()), solicitud);
    }

    @GetMapping("/{id}")
    public DatosCaso consultar(Principal principal, @PathVariable UUID id) {
        return casos.consultar(UUID.fromString(principal.getName()), id);
    }
}
