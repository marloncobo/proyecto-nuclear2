package com.psicoapp.identity.api;

import com.psicoapp.identity.application.IdentityDirectory.Person;
import com.psicoapp.identity.application.IdentityService;
import com.psicoapp.identity.domain.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity")
public class IdentityController {
    public record TeacherRequest(@NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(max = 120) String name, @NotBlank @Size(min = 8, max = 72) String password) {
        @Override
        public String toString() {
            return "TeacherRequest[password=PROTECTED]";
        }
    }
    public record StudentRequest(@NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(max = 120) String name) { }
    public record RoleRequest(@NotNull Role role) { }
    public record PermissionRequest(@NotNull Boolean enabled) { }
    public record CsrfResponse(String headerName, String token) {
        @Override
        public String toString() {
            return "CsrfResponse[token=PROTECTED]";
        }
    }

    private final IdentityService identities;

    public IdentityController(IdentityService identities) {
        this.identities = identities;
    }

    @GetMapping("/me")
    public Person me(Principal principal) {
        return identities.current(UUID.fromString(principal.getName()));
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/teachers")
    @ResponseStatus(HttpStatus.CREATED)
    public Person createTeacher(Principal principal, @Valid @RequestBody TeacherRequest request) {
        return identities.createTeacher(UUID.fromString(principal.getName()), request.email(), request.name(), request.password());
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public Person enrollStudent(Principal principal, @Valid @RequestBody StudentRequest request) {
        return identities.enrollStudent(UUID.fromString(principal.getName()), request.email(), request.name());
    }

    @PutMapping("/users/{userId}/role")
    public Person changeRole(Principal principal, @PathVariable UUID userId, @Valid @RequestBody RoleRequest request) {
        return identities.changeRole(UUID.fromString(principal.getName()), userId, request.role());
    }

    @PutMapping("/roles/{role}/case-creation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setCaseCreation(Principal principal, @PathVariable Role role, @Valid @RequestBody PermissionRequest request) {
        identities.setCaseCreation(UUID.fromString(principal.getName()), role, request.enabled());
    }
}
