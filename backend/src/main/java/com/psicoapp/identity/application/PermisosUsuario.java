package com.psicoapp.identity.application;

import java.util.UUID;

public interface PermisosUsuario {
    boolean puedeCrearCasos(UUID idUsuario);
    void configurarCreacionCasos(UUID idUsuario, boolean habilitado);
    void retirarCreacionCasos(UUID idUsuario);
}
