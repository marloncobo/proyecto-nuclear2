package com.psicoapp.notifications.domain;

/** Las dos representaciones del correo. El contenido no debe aparecer en logs. */
public record ContenidoCorreo(String texto, String html) {
    @Override
    public String toString() {
        return "ContenidoCorreo[datos protegidos]";
    }
}
