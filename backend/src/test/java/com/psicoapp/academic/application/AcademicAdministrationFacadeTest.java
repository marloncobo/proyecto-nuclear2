package com.psicoapp.academic.application;

import com.psicoapp.academic.domain.AcademicSpace;
import com.psicoapp.identity.application.IdentityDirectory;
import com.psicoapp.shared.exceptions.AccessDenied;
import com.psicoapp.shared.exceptions.NotFound;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicAdministrationFacadeTest {
    @Mock AcademicSpaceRepository spaces;
    @Mock IdentityDirectory identities;
    private AcademicAdministrationFacade academic;
    private final UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        academic = new AcademicAdministrationFacade(spaces, identities);
    }

    @Test
    @DisplayName("RF-04 / ER ESPACIO_ACADEMICO: docente registra nombre y periodo")
    void createsAcademicSpaceAfterAuthorizingTeacher() {
        var result = academic.create(teacherId, " Psicologia social ", " 2026-2 ");
        assertEquals("Psicologia social", result.name());
        assertEquals("2026-2", result.period());
        var order = inOrder(identities, spaces);
        order.verify(identities).requireTeacher(teacherId);
        order.verify(spaces).save(new AcademicSpace(result.id(), result.name(), result.period()));
    }

    @Test
    @DisplayName("RF-02 / RF-04: sin rol docente no crea ni consulta espacios")
    void rejectsUnauthorizedUserBeforeAccessingPersistence() {
        doThrow(new AccessDenied("No autorizado")).when(identities).requireTeacher(teacherId);
        assertThrows(AccessDenied.class, () -> academic.create(teacherId, "Psicologia", "2026-2"));
        assertThrows(AccessDenied.class, () -> academic.list(teacherId));
        assertThrows(AccessDenied.class, () -> academic.get(teacherId, UUID.randomUUID()));
        verifyNoInteractions(spaces);
    }

    @Test
    @DisplayName("RF-04: datos incompletos no se persisten")
    void rejectsIncompleteAcademicSpace() {
        assertThrows(IllegalArgumentException.class, () -> academic.create(teacherId, " ", "2026-2"));
        assertThrows(IllegalArgumentException.class, () -> academic.create(teacherId, "Psicologia", ""));
        verify(spaces, never()).save(any());
    }

    @Test
    @DisplayName("RF-04: consulta espacio existente por identificador")
    void returnsExistingSpace() {
        AcademicSpace space = new AcademicSpace(UUID.randomUUID(), "Psicologia", "2026-2");
        when(spaces.findById(space.id())).thenReturn(Optional.of(space));
        assertEquals(new AcademicAdministrationFacade.Space(space.id(), space.name(), space.period()),
                academic.get(teacherId, space.id()));
    }

    @Test
    @DisplayName("RF-04: informa espacio inexistente")
    void reportsMissingSpace() {
        assertThrows(NotFound.class, () -> academic.get(teacherId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("RF-04: lista espacios disponibles sin inventar membresias de estudiantes")
    void listsAcademicSpaces() {
        var list = List.of(new AcademicSpace(UUID.randomUUID(), "Psicologia", "2026-2"));
        when(spaces.findAll()).thenReturn(list);
        assertEquals(List.of(new AcademicAdministrationFacade.Space(list.getFirst().id(), "Psicologia", "2026-2")),
                academic.list(teacherId));
    }
}
