package com.psicoapp.reports.api;

import com.psicoapp.reports.application.FachadaConsultaParticipaciones;
import com.psicoapp.reports.application.FachadaConsultaParticipaciones.ResumenParticipacion;
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
    private final FachadaConsultaParticipaciones participaciones;

    public ControladorReportes(FachadaConsultaParticipaciones participaciones) {
        this.participaciones = participaciones;
    }

    @GetMapping("/casos/{idCaso}/estudiantes/{idEstudiante}")
    public List<ResumenParticipacion> consultarPorEstudiante(Principal principal,
            @PathVariable UUID idCaso, @PathVariable UUID idEstudiante) {
        return participaciones.consultarPorEstudiante(UUID.fromString(principal.getName()), idEstudiante, idCaso);
    }
}
