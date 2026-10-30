package com.psicoapp.identity.application;

import com.psicoapp.identity.domain.Role;
import com.psicoapp.identity.domain.User;
import com.psicoapp.shared.exceptions.AccessDenied;
import com.psicoapp.shared.exceptions.Conflict;
import com.psicoapp.shared.exceptions.NotFound;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {
    @Mock UserRepository users;
    @Mock RolePermissions permissions;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);
    private IdentityService service;
    private User admin;
    private User teacher;

    @BeforeEach
    void setUp() {
        service = new IdentityService(users, passwords, permissions, "universidad.edu.co, otra.edu.co");
        admin = user(Role.ADMINISTRADOR, "admin@universidad.edu.co");
        teacher = user(Role.DOCENTE, "docente@universidad.edu.co");
    }

    @Test
    @DisplayName("RF-03 / RN-10: solo administrador crea docentes y almacena un hash")
    void administratorCreatesTeacherWithHashedPassword() {
        when(users.findById(admin.id())).thenReturn(Optional.of(admin));
        var result = service.createTeacher(admin.id(), " DOCENTE@universidad.edu.co ", " Docente ", "clave-ficticia");
        var saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertEquals("docente@universidad.edu.co", result.email());
        assertEquals("Docente", result.name());
        assertEquals("DOCENTE", result.role());
        assertTrue(saved.getValue().active());
        assertTrue(passwords.matches("clave-ficticia", saved.getValue().passwordHash()));
        assertNotEquals("clave-ficticia", saved.getValue().passwordHash());
    }

    @Test
    @DisplayName("RN-10: docente no puede administrar docentes ni permisos")
    void teacherCannotAdministerUsersOrPermissions() {
        when(users.findById(teacher.id())).thenReturn(Optional.of(teacher));
        assertThrows(AccessDenied.class, () -> service.createTeacher(teacher.id(), "otro@universidad.edu.co", "Otro", "clave-ficticia"));
        assertThrows(AccessDenied.class, () -> service.changeRole(teacher.id(), admin.id(), Role.DOCENTE));
        assertThrows(AccessDenied.class, () -> service.setCaseCreation(teacher.id(), Role.DOCENTE, true));
        verify(users, never()).save(any());
        verifyNoInteractions(permissions);
    }

    @Test
    @DisplayName("RF-03: rechaza correo docente duplicado sin sobrescribirlo")
    void rejectsDuplicateEmail() {
        when(users.findById(admin.id())).thenReturn(Optional.of(admin));
        when(users.findByEmail(teacher.email())).thenReturn(Optional.of(teacher));
        assertThrows(Conflict.class, () -> service.createTeacher(admin.id(), teacher.email(), "Otro", "clave-ficticia"));
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("RF-03: rechaza contrasena corta y BCrypt de mas de 72 bytes")
    void rejectsInvalidPasswords() {
        when(users.findById(admin.id())).thenReturn(Optional.of(admin));
        assertThrows(IllegalArgumentException.class, () -> service.createTeacher(admin.id(), teacher.email(), "Docente", "corta"));
        assertThrows(IllegalArgumentException.class, () -> service.createTeacher(admin.id(), teacher.email(), "Docente", "\u00f1".repeat(40)));
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("RF-02 / RN-10: administrador asigna rol a una cuenta existente")
    void administratorChangesRole() {
        when(users.findById(admin.id())).thenReturn(Optional.of(admin));
        when(users.findById(teacher.id())).thenReturn(Optional.of(teacher));
        assertEquals("ADMINISTRADOR", service.changeRole(admin.id(), teacher.id(), Role.ADMINISTRADOR).role());
        verify(users).save(teacher.withRole(Role.ADMINISTRADOR));
    }

    @Test
    @DisplayName("RF-03: administrador no puede retirar su propio acceso")
    void preventsAdministratorSelfDemotion() {
        when(users.findById(admin.id())).thenReturn(Optional.of(admin));
        assertThrows(Conflict.class, () -> service.changeRole(admin.id(), admin.id(), Role.DOCENTE));
    }

    @Test
    @DisplayName("RF-02 / ER ROL_PERMISO: administrador concede y retira permiso al rol docente")
    void administratorManagesRolePermission() {
        when(users.findById(admin.id())).thenReturn(Optional.of(admin));
        service.setCaseCreation(admin.id(), Role.DOCENTE, true);
        service.setCaseCreation(admin.id(), Role.DOCENTE, false);
        verify(permissions).setCaseCreation(Role.DOCENTE, true);
        verify(permissions).setCaseCreation(Role.DOCENTE, false);
        assertThrows(IllegalArgumentException.class, () -> service.setCaseCreation(admin.id(), Role.ESTUDIANTE, true));
    }

    @Test
    @DisplayName("RF-02: el rol docente por si solo no autoriza crear casos")
    void requiresSpecificPermissionForCases() {
        when(users.findById(teacher.id())).thenReturn(Optional.of(teacher));
        when(permissions.canCreateCases(Role.DOCENTE)).thenReturn(false, true);
        assertThrows(AccessDenied.class, () -> service.requireCaseCreation(teacher.id()));
        assertDoesNotThrow(() -> service.requireCaseCreation(teacher.id()));
    }

    @Test
    @DisplayName("RF-01 / RF-04: docente registra estudiante institucional sin contrasena")
    void enrollsInstitutionalStudent() {
        when(users.findById(teacher.id())).thenReturn(Optional.of(teacher));
        var result = service.enrollStudent(teacher.id(), " ESTUDIANTE@UNIVERSIDAD.EDU.CO ", "Estudiante");
        assertEquals("ESTUDIANTE", result.role());
        assertEquals("estudiante@universidad.edu.co", result.email());
        verify(users).save(argThat(user -> user.passwordHash() == null && user.role() == Role.ESTUDIANTE));
    }

    @Test
    @DisplayName("RF-01: rechaza dominio externo y sufijos que imitan el institucional")
    void rejectsNonInstitutionalEmail() {
        when(users.findById(teacher.id())).thenReturn(Optional.of(teacher));
        assertThrows(IllegalArgumentException.class, () -> service.enrollStudent(teacher.id(), "e@externo.com", "Estudiante"));
        assertThrows(IllegalArgumentException.class, () -> service.enrollStudent(teacher.id(), "e@universidad.edu.co.evil.com", "Estudiante"));
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("RF-04: reutiliza estudiante existente sin duplicar su identidad")
    void reusesExistingStudent() {
        User student = user(Role.ESTUDIANTE, "estudiante@universidad.edu.co");
        when(users.findById(teacher.id())).thenReturn(Optional.of(teacher));
        when(users.findByEmail(student.email())).thenReturn(Optional.of(student));
        assertEquals(student.id(), service.enrollStudent(teacher.id(), student.email(), "Estudiante").id());
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("RF-04: no transforma un correo docente en cuenta estudiante")
    void rejectsStaffEmailForEnrollment() {
        when(users.findById(teacher.id())).thenReturn(Optional.of(teacher));
        when(users.findByEmail(teacher.email())).thenReturn(Optional.of(teacher));
        assertThrows(Conflict.class, () -> service.enrollStudent(teacher.id(), teacher.email(), "Estudiante"));
    }

    @Test
    @DisplayName("RF-02: cuenta inexistente o inactiva no accede como docente")
    void missingAndInactiveUserCannotAccess() {
        assertThrows(NotFound.class, () -> service.requireTeacher(UUID.randomUUID()));
        User inactive = new User(teacher.id(), teacher.email(), teacher.name(), teacher.role(), teacher.passwordHash(), false, teacher.createdAt());
        when(users.findById(teacher.id())).thenReturn(Optional.of(inactive));
        assertThrows(AccessDenied.class, () -> service.requireTeacher(teacher.id()));
    }

    @Test
    @DisplayName("RF-03: arranque no reemplaza credenciales de un administrador existente")
    void bootstrapDoesNotOverwriteExistingAccount() {
        when(users.findByEmail(admin.email())).thenReturn(Optional.of(admin));
        service.initializeAdministrator(admin.email(), "otra-clave-ficticia");
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("RF-03: configura administrador inicial sin credenciales predeterminadas")
    void bootstrapCreatesAdministratorOnlyWhenConfigured() {
        service.initializeAdministrator("", "");
        verifyNoInteractions(users);
        service.initializeAdministrator(admin.email(), "clave-ficticia");
        verify(users).save(argThat(user -> user.role() == Role.ADMINISTRADOR));
    }

    private User user(Role role, String email) {
        return new User(UUID.randomUUID(), email, "Usuario ficticio", role,
                role == Role.ESTUDIANTE ? null : passwords.encode("clave-ficticia"), true, Instant.now());
    }
}
