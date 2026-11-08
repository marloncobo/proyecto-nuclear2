package com.psicoapp.sessions.application;

import com.psicoapp.sessions.domain.Convocatoria;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioConvocatorias {
    void actualizar(Convocatoria convocatoria);
    void guardar(Convocatoria convocatoria);
    List<Convocatoria> listarPorSesion(UUID idSesion);
    Optional<Convocatoria> buscarAutorizada(UUID idEstudiante, String resumenCodigo);
}
