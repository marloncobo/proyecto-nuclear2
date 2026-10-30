package com.psicoapp.identity.api;

import com.psicoapp.academic.api.AcademicSpaceController;
import com.psicoapp.academic.application.AcademicAdministrationFacade;
import com.psicoapp.identity.application.IdentityDirectory.Person;
import com.psicoapp.identity.application.IdentityService;
import com.psicoapp.identity.application.UserRepository;
import com.psicoapp.identity.domain.Role;
import com.psicoapp.identity.domain.User;
import com.psicoapp.identity.infrastructure.IdentityAuthenticationProvider;
import com.psicoapp.identity.infrastructure.IdentityConfiguration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {IdentityController.class, AcademicSpaceController.class}, properties = {
        "psicoapp.identity.admin-email=", "psicoapp.identity.admin-password="
})
@Import({IdentityConfiguration.class, IdentityAuthenticationProvider.class})
class IdentitySecurityTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder passwords;
    @MockitoBean UserRepository users;
    @MockitoBean IdentityService identities;
    @MockitoBean AcademicAdministrationFacade academic;

    @Test
    @DisplayName("RF-02: solicitudes anonimas no consultan datos de identidad")
    void anonymousRequestIsUnauthorized() throws Exception {
        mvc.perform(get("/api/identity/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(identities);
    }

    @Test
    @DisplayName("RF-02: personal con credenciales validas consulta su propia identidad")
    void authenticatesActualBasicCredentials() throws Exception {
        var teacher = new User(UUID.randomUUID(), "docente@uni.edu", "Docente", Role.DOCENTE,
                passwords.encode("clave-ficticia"), true, Instant.now());
        when(users.findByEmail(teacher.email())).thenReturn(Optional.of(teacher));
        when(identities.current(teacher.id())).thenReturn(new Person(teacher.id(), teacher.email(), "Docente", "DOCENTE", false));
        mvc.perform(get("/api/identity/me").with(httpBasic(teacher.email(), "clave-ficticia")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(teacher.id().toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/identity/me").with(httpBasic(teacher.email(), "incorrecta")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-10: docente no crea cuentas docentes aunque envie un identificador valido")
    void teacherCannotCreateTeacher() throws Exception {
        mvc.perform(post("/api/identity/teachers").with(user(UUID.randomUUID().toString()).roles("DOCENTE"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"otro@uni.edu\",\"name\":\"Otro\",\"password\":\"clave-ficticia\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(identities);
    }

    @Test
    @DisplayName("RN-10: administrador crea docente y valida campos HTTP")
    void adminCanCreateTeacherAndInvalidInputIsRejected() throws Exception {
        UUID adminId = UUID.randomUUID();
        when(identities.createTeacher(eq(adminId), anyString(), anyString(), anyString()))
                .thenReturn(new Person(UUID.randomUUID(), "otro@uni.edu", "Otro", "DOCENTE", false));
        mvc.perform(post("/api/identity/teachers").with(user(adminId.toString()).roles("ADMINISTRADOR"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"otro@uni.edu\",\"name\":\"Otro\",\"password\":\"clave-ficticia\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/identity/teachers").with(user(adminId.toString()).roles("ADMINISTRADOR"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalido\",\"name\":\"\",\"password\":\"corta\"}"))
                .andExpect(status().isBadRequest());
        verify(identities, times(1)).createTeacher(eq(adminId), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("RF-02: mutaciones autenticadas requieren token CSRF")
    void rejectsMutationWithoutCsrf() throws Exception {
        mvc.perform(post("/api/academic/spaces").with(user(UUID.randomUUID().toString()).roles("DOCENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Psicologia\",\"period\":\"2026-2\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(academic);
    }

    @Test
    @DisplayName("RF-02 / RF-04: estudiante no administra espacios academicos")
    void studentCannotManageAcademicSpaces() throws Exception {
        mvc.perform(get("/api/academic/spaces").with(user(UUID.randomUUID().toString()).roles("ESTUDIANTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(academic);
    }
}
