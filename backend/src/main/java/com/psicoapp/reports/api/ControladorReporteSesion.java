package com.psicoapp.reports.api;

import com.psicoapp.reports.infrastructure.ReporteSesionJdbc;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reportes/sesiones")
public class ControladorReporteSesion {
    private final ReporteSesionJdbc reportes;

    public ControladorReporteSesion(ReporteSesionJdbc reportes) {
        this.reportes = reportes;
    }

    @GetMapping("/{sesion}")
    public List<ReporteSesionJdbc.Fila> consultar(Principal principal, @PathVariable UUID sesion) {
        return reportes.consultar(UUID.fromString(principal.getName()), sesion);
    }
}
