package com.psicoapp.academic.application;

import com.psicoapp.academic.domain.AcademicSpace;
import com.psicoapp.identity.application.IdentityDirectory;
import com.psicoapp.shared.exceptions.NotFound;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-04: bounded facade coordinating identity authorization and academic spaces. */
@Service
@Transactional
public class AcademicAdministrationFacade {
    public record Space(UUID id, String name, String period) { }

    private final AcademicSpaceRepository spaces;
    private final IdentityDirectory identities;

    public AcademicAdministrationFacade(AcademicSpaceRepository spaces, IdentityDirectory identities) {
        this.spaces = spaces;
        this.identities = identities;
    }

    public Space create(UUID teacherId, String name, String period) {
        identities.requireTeacher(teacherId);
        AcademicSpace space = new AcademicSpace(UUID.randomUUID(), name, period);
        spaces.save(space);
        return view(space);
    }

    @Transactional(readOnly = true)
    public Space get(UUID teacherId, UUID id) {
        identities.requireTeacher(teacherId);
        return spaces.findById(id).map(this::view)
                .orElseThrow(() -> new NotFound("Espacio academico no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Space> list(UUID teacherId) {
        identities.requireTeacher(teacherId);
        return spaces.findAll().stream().map(this::view).toList();
    }

    private Space view(AcademicSpace space) {
        return new Space(space.id(), space.name(), space.period());
    }
}
