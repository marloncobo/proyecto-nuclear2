package com.psicoapp.notifications.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Cuerpo del correo y destinatario fijo opcional. Los datos no deben aparecer en logs. */
public record ContenidoCorreo(String texto, String html,
        @JsonInclude(JsonInclude.Include.NON_NULL) String destinatario) {
    public ContenidoCorreo(String texto, String html) {
        this(texto, html, null);
    }

    @Override
    public String toString() {
        return "ContenidoCorreo[datos protegidos]";
    }
}
