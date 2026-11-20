package com.psicoapp.identity.application;

import com.psicoapp.identity.domain.Rol;
import java.util.Locale;

/** Criterios de consulta; sin dependencias de HTTP, Spring o SQL. */
public record FiltroUsuarios(Rol rol, Boolean activo, String busqueda) {
    public FiltroUsuarios {
        busqueda = busqueda == null ? "" : busqueda.strip();
        if (busqueda.length() > 180) {
            throw new IllegalArgumentException("La busqueda admite hasta 180 caracteres");
        }
        busqueda = busqueda.toLowerCase(Locale.ROOT);
    }
}
