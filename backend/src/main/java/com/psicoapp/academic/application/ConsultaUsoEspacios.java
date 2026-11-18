package com.psicoapp.academic.application;

import java.util.UUID;

/** Consulta actual del uso de un espacio, bajo el bloqueo transaccional del catalogo. */
public interface ConsultaUsoEspacios {
    boolean tieneSesiones(UUID idEspacio);
}
