package com.psicoapp.notifications.infrastructure;

import com.psicoapp.notifications.application.PuertoCorreo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class CorreoSmtp implements PuertoCorreo {
    private final ObjectProvider<JavaMailSender> proveedor;
    private final String remitente;
    public CorreoSmtp(ObjectProvider<JavaMailSender> proveedor,
            @Value("${psicoapp.notificaciones.remitente:}") String remitente) {
        this.proveedor = proveedor; this.remitente = remitente;
    }
    @Override public void enviar(String destinatario, String asunto, String mensaje) {
        JavaMailSender correo = proveedor.getIfAvailable();
        if (correo == null || remitente.isBlank()) throw new IllegalStateException("Correo sin configurar");
        SimpleMailMessage solicitud = new SimpleMailMessage();
        solicitud.setFrom(remitente); solicitud.setTo(destinatario);
        solicitud.setSubject(asunto); solicitud.setText(mensaje);
        correo.send(solicitud);
    }
}
