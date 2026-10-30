package com.psicoapp.academic.application;

import com.psicoapp.academic.domain.AcademicSpace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicSpaceRepository {
    void save(AcademicSpace space);
    Optional<AcademicSpace> findById(UUID id);
    List<AcademicSpace> findAll();
}