package com.psicoapp;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Emite un JWT real para recorridos HTTP de integración. */
public final class AutenticacionPruebas {
    private AutenticacionPruebas() { }

    public static RequestPostProcessor portador(MockMvc mvc, String correo, String clave) {
        try {
            String cuerpo = "{\"correo\":\"" + correo + "\",\"clave\":\"" + clave + "\"}";
            var respuesta = mvc.perform(post("/api/identidad/ingreso")
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andReturn().getResponse();
            if (respuesta.getStatus() != 200) {
                throw new IllegalStateException("No se pudo emitir JWT de prueba: HTTP " + respuesta.getStatus());
            }
            String token = JsonPath.read(respuesta.getContentAsString(), "$.token");
            return solicitud -> {
                solicitud.addHeader("Authorization", "Bearer " + token);
                return solicitud;
            };
        } catch (Exception excepcion) {
            throw new IllegalStateException("No se pudo autenticar la solicitud de prueba", excepcion);
        }
    }
}
