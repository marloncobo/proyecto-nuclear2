package com.psicoapp.notifications.infrastructure;

import com.jayway.jsonpath.JsonPath;
import com.psicoapp.notifications.domain.ContenidoCorreo;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.util.Base64;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PruebaCorreoGmail {
    private static final String URL_TOKEN = "https://oauth2.googleapis.com/token";
    private static final String URL_ENVIO = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
    private final RestClient.Builder constructor = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(constructor).build();

    @Test
    void enviaMimeUnicodeYReutilizaToken() throws Exception {
        var mensaje = new ContenidoCorreo("Sesión de psicología", "<p>Sesión de psicología</p>");
        esperarToken();
        servidor.expect(times(2), requestTo(URL_ENVIO))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-ficticio"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(solicitud -> {
                    String contenido = ((MockClientHttpRequest) solicitud).getBodyAsString();
                    String codificado = JsonPath.read(contenido, "$.raw");
                    try {
                        var correo = new MimeMessage(Session.getInstance(new Properties()),
                                new ByteArrayInputStream(Base64.getUrlDecoder().decode(codificado)));
                        assertEquals("avisos@universidad.example", correo.getFrom()[0].toString());
                        assertEquals("estudiante@universidad.example", correo.getAllRecipients()[0].toString());
                        assertEquals("Convocatoria académica", correo.getSubject());
                        assertTrue(correo.isMimeType("multipart/alternative"));
                        var alternativas = (MimeMultipart) correo.getContent();
                        assertEquals(2, alternativas.getCount());
                        var texto = alternativas.getBodyPart(0);
                        assertTrue(texto.isMimeType("text/plain"));
                        assertEquals(mensaje.texto(), texto.getContent());
                        var html = alternativas.getBodyPart(1);
                        assertTrue(html.isMimeType("text/html"));
                        assertEquals(mensaje.html(), html.getContent());
                    } catch (jakarta.mail.MessagingException excepcion) {
                        throw new AssertionError(excepcion);
                    }
                })
                .andRespond(withSuccess("{\"id\":\"correo-ficticio\"}", MediaType.APPLICATION_JSON));

        var correo = crearCorreo("renovacion-ficticia");
        correo.enviar("estudiante@universidad.example", "Convocatoria académica", mensaje);
        correo.enviar("estudiante@universidad.example", "Convocatoria académica", mensaje);
        servidor.verify();
    }

    @Test
    void rechazoDeGmailDevuelveFalloSinExponerLaRespuesta() {
        esperarToken();
        servidor.expect(requestTo(URL_ENVIO))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("detalle privado"));
        var correo = crearCorreo("renovacion-ficticia");
        var fallo = assertThrows(IllegalStateException.class, () ->
                correo.enviar("estudiante@universidad.example", "Aviso", new ContenidoCorreo("Mensaje", null)));
        assertFalse(fallo.getMessage().contains("detalle privado"));
        assertNull(fallo.getCause());
        servidor.verify();
    }

    @Test
    void sinTokenDeRenovacionNoHaceSolicitudes() {
        var correo = crearCorreo("");
        assertThrows(IllegalStateException.class, () ->
                correo.enviar("estudiante@universidad.example", "Aviso", new ContenidoCorreo("Mensaje", null)));
        servidor.verify();
    }

    private void esperarToken() {
        servidor.expect(requestTo(URL_TOKEN))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string("grant_type=refresh_token&client_id=cliente-ficticio"
                        + "&client_secret=secreto-ficticio&refresh_token=renovacion-ficticia"))
                .andRespond(withSuccess("{\"access_token\":\"token-ficticio\",\"expires_in\":3600}",
                        MediaType.APPLICATION_JSON));
    }

    private CorreoGmail crearCorreo(String tokenRenovacion) {
        return new CorreoGmail(constructor.build(), Clock.systemUTC(), "avisos@universidad.example",
                "cliente-ficticio", "secreto-ficticio", tokenRenovacion);
    }
}
