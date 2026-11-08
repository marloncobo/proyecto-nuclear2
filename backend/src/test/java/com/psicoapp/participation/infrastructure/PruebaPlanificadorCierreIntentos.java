package com.psicoapp.participation.infrastructure;

import com.psicoapp.participation.application.ServicioGestionParticipacion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class PruebaPlanificadorCierreIntentos {
    @Test
    void delegaElCierreUsandoElReloj() {
        ServicioGestionParticipacion participaciones = mock(ServicioGestionParticipacion.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);

        new PlanificadorCierreIntentos(participaciones, reloj).cerrarVencidos();

        verify(participaciones).finalizarVencidos(Instant.parse("2026-01-01T10:00:00Z"));
    }
}
