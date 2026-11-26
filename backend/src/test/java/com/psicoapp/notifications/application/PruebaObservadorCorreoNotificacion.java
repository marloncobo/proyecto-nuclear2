package com.psicoapp.notifications.application;

import com.psicoapp.shared.events.ConvocatoriaModificada;
import com.psicoapp.shared.events.SesionProgramada;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PruebaObservadorCorreoNotificacion {
    private final ObservadorCorreoNotificacion observador = new ObservadorCorreoNotificacion();

    @Test
    void convocatoriaMuestraLosDatosEnColombiaYEscapaElTextoDelDocente() {
        var inicio = Instant.parse("2026-11-27T03:00:00Z");
        var evento = new SesionProgramada(UUID.randomUUID(), inicio, UUID.randomUUID(),
                UUID.randomUUID(), inicio, inicio.plusSeconds(10800), 40, "Caso <A> & grupo",
                "Aula «Norte»", "Primera línea\r\n<ejemplo> & segunda línea",
                URI.create("https://campus.example/ingreso?origen=correo"), "codigo-ficticio");

        var correo = observador.actualizar(evento).getFirst();

        assertTrue(correo.mensaje().contains("Apertura (hora de Colombia): 26/11/2026, 22:00"));
        assertTrue(correo.mensaje().contains("Cierre (hora de Colombia): 27/11/2026, 01:00"));
        assertTrue(correo.mensaje().contains("https://campus.example/ingreso?origen=correo&sesion=" + evento.idSesion()));
        assertTrue(correo.html().contains("Caso &lt;A&gt; &amp; grupo"));
        assertTrue(correo.html().contains("Primera línea<br>&lt;ejemplo&gt; &amp; segunda línea"));
        assertTrue(correo.html().contains("href=\"https://campus.example/ingreso?origen=correo&amp;sesion="
                + evento.idSesion() + "\""));
        assertFalse(correo.html().contains("codigo-ficticio"));
    }

    @Test
    void retiroIdentificaElCasoYSesionSinBotonDeAcceso() {
        var retiro = new ConvocatoriaModificada(UUID.randomUUID(), Instant.EPOCH, UUID.randomUUID(),
                UUID.randomUUID(), ConvocatoriaModificada.TipoCambio.RETIRO, null, "Caso formativo");

        var correo = observador.actualizar(retiro).getFirst();

        assertEquals("Retiro de convocatoria", correo.asunto());
        assertTrue(correo.mensaje().contains("Caso: Caso formativo"));
        assertTrue(correo.html().contains(retiro.idSesion().toString()));
        assertFalse(correo.html().contains("<a "));
    }
}
