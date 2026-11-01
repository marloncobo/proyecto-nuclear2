package com.psicoapp.identity.application;

import java.util.UUID;

/** Las convocatorias verifican credenciales; participacion comprueba su propia autorizacion. */
public interface VerificadorAccesoEstudiante {
    boolean verificar(UUID idEstudiante, String codigo);
}
