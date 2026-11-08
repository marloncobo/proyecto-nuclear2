package com.psicoapp.notifications.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PruebaCorreoSmtp {
    @Test void configuraDestinatarioRemitenteAsuntoYCuerpo() {
        var fabrica = new DefaultListableBeanFactory();
        var servidor = mock(JavaMailSender.class);
        fabrica.registerSingleton("correo", servidor);
        var correo = new CorreoSmtp(fabrica.getBeanProvider(JavaMailSender.class), "avisos@universidad.example");
        correo.enviar("estudiante@universidad.example", "Convocatoria", "Codigo ficticio");
        var mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(servidor).send(mensaje.capture());
        assertEquals("avisos@universidad.example", mensaje.getValue().getFrom());
        assertArrayEquals(new String[]{"estudiante@universidad.example"}, mensaje.getValue().getTo());
        assertEquals("Convocatoria", mensaje.getValue().getSubject());
        assertEquals("Codigo ficticio", mensaje.getValue().getText());
    }
    @Test void faltaDeConfiguracionEsUnFalloReintentable() {
        var fabrica = new DefaultListableBeanFactory();
        var correo = new CorreoSmtp(fabrica.getBeanProvider(JavaMailSender.class), "");
        assertThrows(IllegalStateException.class, () -> correo.enviar("alumno@universidad.example", "Aviso", "Mensaje"));
    }
}
