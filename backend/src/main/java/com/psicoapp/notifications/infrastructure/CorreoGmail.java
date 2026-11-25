package com.psicoapp.notifications.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.psicoapp.notifications.application.PuertoCorreo;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CorreoGmail implements PuertoCorreo {
    private static final String URL_TOKEN = "https://oauth2.googleapis.com/token";
    private static final String URL_ENVIO = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
    private static final String UTF_8 = StandardCharsets.UTF_8.name();
    private final RestClient cliente;
    private final Clock reloj;
    private final String remitente;
    private final String idCliente;
    private final String secretoCliente;
    private final String tokenRenovacion;
    private String tokenAcceso;
    private Instant renovarDesde = Instant.MIN;

    @Autowired
    public CorreoGmail(Clock reloj,
            @Value("${psicoapp.notificaciones.remitente}") String remitente,
            @Value("${psicoapp.notificaciones.gmail.id-cliente}") String idCliente,
            @Value("${psicoapp.notificaciones.gmail.secreto-cliente}") String secretoCliente,
            @Value("${psicoapp.notificaciones.gmail.token-renovacion}") String tokenRenovacion) {
        this(construirCliente(), reloj, remitente, idCliente, secretoCliente, tokenRenovacion);
    }

    CorreoGmail(RestClient cliente, Clock reloj, String remitente, String idCliente,
            String secretoCliente, String tokenRenovacion) {
        this.cliente = cliente;
        this.reloj = reloj;
        this.remitente = remitente;
        this.idCliente = idCliente;
        this.secretoCliente = secretoCliente;
        this.tokenRenovacion = tokenRenovacion;
    }

    private static RestClient construirCliente() {
        var solicitudes = new SimpleClientHttpRequestFactory();
        solicitudes.setConnectTimeout(5000);
        solicitudes.setReadTimeout(5000);
        return RestClient.builder().requestFactory(solicitudes).build();
    }

    @Override
    public void enviar(String destinatario, String asunto, String mensaje) {
        if (remitente.isBlank() || idCliente.isBlank() || secretoCliente.isBlank() || tokenRenovacion.isBlank()) {
            throw new IllegalStateException("Gmail sin configurar");
        }
        try {
            String contenido = codificarMensaje(destinatario, asunto, mensaje);
            String token = obtenerToken();
            try {
                cliente.post().uri(URL_ENVIO)
                        .headers(cabeceras -> cabeceras.setBearerAuth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("raw", contenido))
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientResponseException excepcion) {
                if (excepcion.getStatusCode().value() == 401) invalidarToken(token);
                throw excepcion;
            }
        } catch (RestClientException | MessagingException | IOException excepcion) {
            throw new IllegalStateException("No se pudo enviar el correo mediante Gmail");
        }
    }

    private String codificarMensaje(String destinatario, String asunto, String mensaje)
            throws MessagingException, IOException {
        var correo = new MimeMessage(Session.getInstance(new Properties()));
        correo.setFrom(new InternetAddress(remitente, true));
        correo.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario, true));
        correo.setSubject(asunto, UTF_8);
        correo.setText(mensaje, UTF_8);
        var contenido = new ByteArrayOutputStream();
        correo.writeTo(contenido);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(contenido.toByteArray());
    }

    private synchronized String obtenerToken() {
        Instant ahora = reloj.instant();
        if (ahora.isBefore(renovarDesde)) return tokenAcceso;

        var formulario = new LinkedMultiValueMap<String, String>();
        formulario.add("grant_type", "refresh_token");
        formulario.add("client_id", idCliente);
        formulario.add("client_secret", secretoCliente);
        formulario.add("refresh_token", tokenRenovacion);
        var respuesta = cliente.post().uri(URL_TOKEN)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .body(RespuestaToken.class);
        if (respuesta == null || respuesta.token() == null || respuesta.token().isBlank()
                || respuesta.vigenciaSegundos() <= 0) {
            throw new IllegalStateException("Google no devolvio un token de acceso vigente");
        }
        tokenAcceso = respuesta.token();
        renovarDesde = ahora.plusSeconds(Math.max(0, respuesta.vigenciaSegundos() - 60));
        return tokenAcceso;
    }

    private synchronized void invalidarToken(String token) {
        if (token.equals(tokenAcceso)) renovarDesde = Instant.MIN;
    }

    private record RespuestaToken(@JsonProperty("access_token") String token,
            @JsonProperty("expires_in") long vigenciaSegundos) { }
}
