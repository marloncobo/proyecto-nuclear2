package com.psicoapp.notifications.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PruebaNotificacion {
    @Test
    void exigeLosDatosMinimos() {
        assertThrows(IllegalArgumentException.class, () -> new Notificacion(UUID.randomUUID(),
                UUID.randomUUID(), "", TipoNotificacion.CONVOCATORIA, "Asunto", "Mensaje",
                null, "codigo", Instant.now(), EstadoNotificacion.PENDIENTE));
    }

    @Test
    void conservaContenidoAlMarcarEntrega() {
        Notificacion original = new Notificacion(UUID.randomUUID(), UUID.randomUUID(),
                "estudiante@universidad.edu.co", TipoNotificacion.RESULTADO, "Resultado",
                "La calificacion esta disponible", "/resultados/1", null, Instant.now(),
                EstadoNotificacion.PENDIENTE);

        Notificacion enviada = original.marcar(EstadoNotificacion.ENVIADA);

        assertEquals(original.id(), enviada.id());
        assertEquals(EstadoNotificacion.ENVIADA, enviada.estado());
        assertEquals(original.mensaje(), enviada.mensaje());
    }
}
