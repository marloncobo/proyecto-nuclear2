package com.psicoapp.academic.application;

import com.psicoapp.academic.domain.EspacioAcademico;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioEspaciosAcademicos {
    Optional<EspacioAcademico> bloquearPorId(UUID id);
    void actualizar(EspacioAcademico espacio);
    void guardar(EspacioAcademico espacio);
    Optional<EspacioAcademico> buscarPorId(UUID id);
    List<EspacioAcademico> listarTodos();
}
