package com.psicoapp.notifications.application;

public interface PuertoCorreo {
    void enviar(String destinatario, String asunto, String mensaje);
}
