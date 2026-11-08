package com.psicoapp.sessions.infrastructure;

import com.psicoapp.sessions.application.ServicioGestionSesiones;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class PruebaPlanificadorCierreSesiones {
    @Test
    @DisplayName("RN-12: planificador delega al caso de uso con reloj inyectado")
    void delegaCierreSinAccederASql() {
        var servicio = mock(ServicioGestionSesiones.class);
        var ahora = Instant.parse("2026-11-02T15:00:00Z");
        new PlanificadorCierreSesiones(servicio, Clock.fixed(ahora, ZoneOffset.UTC)).cerrarVencidas();
        verify(servicio).cerrarVencidas(ahora);
        verifyNoMoreInteractions(servicio);
    }
}
