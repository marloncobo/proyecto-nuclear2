package com.psicoapp.identity.application;

import com.psicoapp.identity.domain.Role;
import com.psicoapp.identity.domain.User;
import com.psicoapp.shared.exceptions.AccessDenied;
import com.psicoapp.shared.exceptions.Conflict;
import com.psicoapp.shared.exceptions.NotFound;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IdentityService implements IdentityDirectory {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final RolePermissions permissions;
    private final Set<String> institutionalDomains;

    public IdentityService(UserRepository users, PasswordEncoder passwords, RolePermissions permissions,
            @Value("${psicoapp.identity.institutional-domains}") String domains) {
        this.users = users;
        this.passwords = passwords;
        this.permissions = permissions;
        this.institutionalDomains = Arrays.stream(domains.split(","))
                .map(String::strip).filter(s -> !s.isEmpty()).map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public Person createTeacher(UUID actorId, String email, String name, String password) {
        requireRole(actorId, Role.ADMINISTRADOR);
        return create(email, name, Role.DOCENTE, password);
    }

    public Person changeRole(UUID actorId, UUID userId, Role role) {
        requireRole(actorId, Role.ADMINISTRADOR);
        if (actorId.equals(userId) && role != Role.ADMINISTRADOR) {
            throw new Conflict("No puede retirar su propio rol de administrador");
        }
        User updated = get(userId).withRole(role);
        if (role == Role.ESTUDIANTE) {
            validateInstitutionalEmail(updated.email());
        }
        users.save(updated);
        return person(updated);
    }

    public void setCaseCreation(UUID actorId, Role role, boolean enabled) {
        requireRole(actorId, Role.ADMINISTRADOR);
        if (role != Role.DOCENTE) {
            throw new IllegalArgumentException("El permiso de creación de casos corresponde al rol docente");
        }
        permissions.setCaseCreation(role, enabled);
    }

    @Override
    public Person enrollStudent(UUID teacherId, String email, String name) {
        requireRole(teacherId, Role.DOCENTE);
        String normalized = validateInstitutionalEmail(email);
        return users.findByEmail(normalized).map(existing -> {
            if (!existing.active() || existing.role() != Role.ESTUDIANTE) {
                throw new Conflict("El correo corresponde a una cuenta inactiva o de personal del sistema");
            }
            return person(existing);
        }).orElseGet(() -> create(normalized, name, Role.ESTUDIANTE, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Person requireTeacher(UUID userId) {
        return requireRole(userId, Role.DOCENTE);
    }

    @Transactional(readOnly = true)
    public Person requireRole(UUID userId, Role role) {
        User user = get(userId);
        if (!user.active() || user.role() != role) {
            throw new AccessDenied("El rol no permite esta operación");
        }
        return person(user);
    }

    @Transactional(readOnly = true)
    public Person current(UUID userId) {
        return person(get(userId));
    }

    @Transactional(readOnly = true)
    public void requireCaseCreation(UUID userId) {
        Person user = requireRole(userId, Role.DOCENTE);
        if (!user.canCreateCases()) {
            throw new AccessDenied("El docente no tiene permiso para crear casos");
        }
    }

    public void initializeAdministrator(String email, String password) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        String normalized = User.normalizeEmail(email);
        var existing = users.findByEmail(normalized);
        if (existing.isEmpty()) {
            create(normalized, "Administrador", Role.ADMINISTRADOR, password);
        } else if (!existing.get().active() || existing.get().role() != Role.ADMINISTRADOR) {
            throw new Conflict("El correo de arranque ya pertenece a una cuenta que no es administrador activo");
        }
    }

    private Person create(String email, String name, Role role, String password) {
        String normalized = User.normalizeEmail(email);
        if (users.findByEmail(normalized).isPresent()) {
            throw new Conflict("El correo ya está registrado");
        }
        String hash = null;
        if (role != Role.ESTUDIANTE) {
            if (password == null || password.length() < 8 || password.length() > 72
                    || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres y hasta 72 bytes");
            }
            hash = passwords.encode(password);
        }
        User user = new User(UUID.randomUUID(), normalized, name, role, hash, true, java.time.Instant.now());
        users.save(user);
        return person(user);
    }

    private String validateInstitutionalEmail(String email) {
        String normalized = User.normalizeEmail(email);
        if (!institutionalDomains.contains(normalized.substring(normalized.indexOf('@') + 1))) {
            throw new IllegalArgumentException("El correo debe pertenecer a un dominio institucional configurado");
        }
        return normalized;
    }

    private User get(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFound("Usuario no encontrado"));
    }

    private Person person(User user) {
        return new Person(user.id(), user.email(), user.name(), user.role().name(),
                user.role() == Role.DOCENTE && permissions.canCreateCases(user.role()));
    }
}
