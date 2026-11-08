package com.psicoapp.participation.application;

import com.psicoapp.participation.domain.Intento;
import java.util.List;
import java.util.UUID;

/** Puerto de lectura separado de las operaciones de escritura de intentos. */
public interface RepositorioConsultaIntentos {
    List<Intento> buscarTerminadosPorEstudianteYCaso(UUID idEstudiante, UUID idCaso);
}
