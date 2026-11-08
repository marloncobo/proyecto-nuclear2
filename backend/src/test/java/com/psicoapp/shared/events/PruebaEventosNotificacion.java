package com.psicoapp.shared.events;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PruebaEventosNotificacion {
    // CP-NO-001: RF-20/RF-21, CU-14; proteger el codigo incluso en toString.
    @Test
    void convocatoriaNoExponeCodigoNiMensajeEnRepresentacion() {
        var evento = convocatoria(URI.create("https://universidad.example/ingreso"), "codigo-ficticio");
        assertEquals("codigo-ficticio", evento.codigoAcceso());
        assertFalse(evento.toString().contains("codigo-ficticio"));
        assertFalse(evento.toString().contains("Mensaje reservado"));
    }

    @Test
    void convocatoriaRechazaUrlSinCanalWebOCredencialesIncrustadas() {
        assertThrows(IllegalArgumentException.class,
                () -> convocatoria(URI.create("javascript:alert(1)"), "codigo-ficticio"));
        assertThrows(IllegalArgumentException.class,
                () -> convocatoria(URI.create("https://usuario:clave@universidad.example"), "codigo-ficticio"));
        assertThrows(IllegalArgumentException.class,
                () -> convocatoria(URI.create("https://universidad.example"), " "));
    }

    // CP-NO-002: RF-27/CU-33; una nota invalida no forma un evento de resultado.
    @Test
    void resultadoRechazaNotaNegativaYConservaPrecision() {
        UUID estudiante = UUID.randomUUID();
        UUID sesion = UUID.randomUUID();
        UUID intento = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new ResultadosPublicados(
                UUID.randomUUID(), Instant.EPOCH, estudiante, sesion, intento, new BigDecimal("-0.1")));
        var evento = new ResultadosPublicados(UUID.randomUUID(), Instant.EPOCH,
                estudiante, sesion, intento, new BigDecimal("4.25"));
        assertEquals(new BigDecimal("4.25"), evento.calificacion());
        assertEquals(intento, evento.idIntento());
    }

    // CP-NO-003: RF-21/CU-34; retiro sin credenciales y alta del mismo convocado.
    @Test
    void cambioValidaDestinatarioYRetiroSinCredenciales() {
        var datos = convocatoria(URI.create("https://universidad.example"), "codigo-ficticio");
        assertDoesNotThrow(() -> new ConvocatoriaModificada(UUID.randomUUID(), Instant.EPOCH,
                datos.idEstudiante(), datos.idSesion(), ConvocatoriaModificada.TipoCambio.ALTA, datos));
        assertThrows(IllegalArgumentException.class, () -> new ConvocatoriaModificada(
                UUID.randomUUID(), Instant.EPOCH, UUID.randomUUID(), datos.idSesion(),
                ConvocatoriaModificada.TipoCambio.ALTA, datos));
        assertThrows(IllegalArgumentException.class, () -> new ConvocatoriaModificada(
                UUID.randomUUID(), Instant.EPOCH, datos.idEstudiante(), datos.idSesion(),
                ConvocatoriaModificada.TipoCambio.RETIRO, datos));
        assertDoesNotThrow(() -> new ConvocatoriaModificada(UUID.randomUUID(), Instant.EPOCH,
                datos.idEstudiante(), datos.idSesion(), ConvocatoriaModificada.TipoCambio.RETIRO, null));
    }

    private SesionProgramada convocatoria(URI url, String codigo) {
        return new SesionProgramada(UUID.randomUUID(), Instant.EPOCH, UUID.randomUUID(),
                UUID.randomUUID(), "Caso formativo", "Mensaje reservado", url, codigo);
    }
}
