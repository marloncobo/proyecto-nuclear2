package com.psicoapp.notifications.application;

import com.psicoapp.notifications.domain.ContenidoCorreo;

public interface PuertoCorreo {
    void enviar(String destinatario, String asunto, ContenidoCorreo contenido);
}
