package com.psicoapp.reports.api;

import com.psicoapp.reports.application.FachadaConsultaParticipaciones;
import com.psicoapp.reports.application.FachadaConsultaParticipaciones.ResumenParticipacion;
import com.psicoapp.evaluation.application.GestionValoraciones.ValoracionPublicada;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ControladorReportes {
    public record SolicitudValoracion(String mensaje) { }
    private final FachadaConsultaParticipaciones participaciones;

    public ControladorReportes(FachadaConsultaParticipaciones participaciones) {
        this.participaciones = participaciones;
    }

    @GetMapping("/casos/{idCaso}/estudiantes/{idEstudiante}")
    public List<ResumenParticipacion> consultarPorEstudiante(Principal principal,
            @PathVariable UUID idCaso, @PathVariable UUID idEstudiante) {
        return participaciones.consultarPorEstudiante(UUID.fromString(principal.getName()), idEstudiante, idCaso);
    }

    @PostMapping("/participaciones/{idIntento}/valoraciones")
    @ResponseStatus(HttpStatus.CREATED)
    public ValoracionPublicada registrarValoracion(Principal principal, @PathVariable UUID idIntento,
            @RequestBody SolicitudValoracion solicitud) {
        return participaciones.registrarValoracion(UUID.fromString(principal.getName()), idIntento, solicitud.mensaje());
    }
}
