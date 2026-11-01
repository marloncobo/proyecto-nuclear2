package com.psicoapp.cases.api;

import com.psicoapp.cases.application.ServicioPublicacionCaso;
import com.psicoapp.cases.application.ServicioPublicacionCaso.DatosPublicacion;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/casos")
public class ControladorPublicacionCasos {
    private final ServicioPublicacionCaso publicacion;

    public ControladorPublicacionCasos(ServicioPublicacionCaso publicacion) {
        this.publicacion = publicacion;
    }

    @PostMapping("/{id}/publicar")
    public DatosPublicacion publicar(Principal principal, @PathVariable UUID id) {
        return publicacion.publicar(UUID.fromString(principal.getName()), id);
    }
}
