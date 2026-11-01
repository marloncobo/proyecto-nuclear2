package com.psicoapp.cases.domain;

import java.util.UUID;

public record OpcionRespuesta(UUID id, String texto, boolean esCorrecta, String justificacion) {
    public OpcionRespuesta {
        ContenidoCaso.identificador(id);
        texto = ContenidoCaso.texto(texto, 16000);
        justificacion = ContenidoCaso.texto(justificacion, 16000);
    }
}
