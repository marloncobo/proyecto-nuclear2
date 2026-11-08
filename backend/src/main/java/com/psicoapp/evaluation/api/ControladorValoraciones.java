package com.psicoapp.evaluation.api;

import com.psicoapp.evaluation.application.*;
import java.security.Principal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluaciones/{idIntento}/valoraciones")
public class ControladorValoraciones {
    public record DatosValoracion(String mensaje) { }
    private final ServicioValoraciones valoraciones;
    public ControladorValoraciones(ServicioValoraciones valoraciones) { this.valoraciones = valoraciones; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RepositorioValoraciones.Valoracion registrar(Principal principal, @PathVariable UUID idIntento,
            @RequestBody DatosValoracion datos) {
        return valoraciones.registrar(UUID.fromString(principal.getName()), idIntento, datos.mensaje());
    }
    @GetMapping
    public List<RepositorioValoraciones.Valoracion> consultar(Principal principal, @PathVariable UUID idIntento) {
        return valoraciones.consultar(UUID.fromString(principal.getName()), idIntento);
    }
}
