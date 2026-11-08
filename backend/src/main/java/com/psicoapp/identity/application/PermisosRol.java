package com.psicoapp.identity.application;

import com.psicoapp.identity.domain.Rol;

/** El modelo ER asigna permisos a roles, no a usuarios individuales. */
public interface PermisosRol {
    boolean puedeCrearCasos(Rol rol);
    void configurarCreacionCasos(Rol rol, boolean habilitado);
}
