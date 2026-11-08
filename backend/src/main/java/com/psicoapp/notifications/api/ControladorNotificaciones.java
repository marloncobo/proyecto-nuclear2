package com.psicoapp.notifications.api;

import com.psicoapp.notifications.application.*;
import java.security.Principal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificaciones")
public class ControladorNotificaciones {
    private final ServicioAvisos avisos;
    public ControladorNotificaciones(ServicioAvisos avisos) { this.avisos = avisos; }
    @GetMapping
    public List<RepositorioAvisos.Aviso> listar(Principal principal,
            @RequestParam(defaultValue = "20") int limite, @RequestParam(defaultValue = "0") int desplazamiento) {
        return avisos.listar(UUID.fromString(principal.getName()), limite, desplazamiento);
    }
    @PutMapping("/{id}/leida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLeida(Principal principal, @PathVariable UUID id) {
        avisos.marcarLeida(UUID.fromString(principal.getName()), id);
    }
}
