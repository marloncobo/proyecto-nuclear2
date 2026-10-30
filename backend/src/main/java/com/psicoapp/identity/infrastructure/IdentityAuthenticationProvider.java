package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.StudentAccessVerifier;
import com.psicoapp.identity.application.UserRepository;
import com.psicoapp.identity.domain.Role;
import com.psicoapp.identity.domain.User;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class IdentityAuthenticationProvider implements AuthenticationProvider {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final ObjectProvider<StudentAccessVerifier> studentAccess;

    public IdentityAuthenticationProvider(UserRepository users, PasswordEncoder passwords,
            ObjectProvider<StudentAccessVerifier> studentAccess) {
        this.users = users;
        this.passwords = passwords;
        this.studentAccess = studentAccess;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String email;
        try {
            email = User.normalizeEmail(authentication.getName());
        } catch (IllegalArgumentException error) {
            throw invalidCredentials();
        }
        User user = users.findByEmail(email).filter(User::active).orElseThrow(this::invalidCredentials);
        String secret = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
        boolean valid;
        if (user.role() == Role.ESTUDIANTE) {
            StudentAccessVerifier verifier = studentAccess.getIfAvailable();
            valid = verifier != null && verifier.verifies(user.id(), secret);
        } else {
            valid = secret.getBytes(StandardCharsets.UTF_8).length <= 72
                    && passwords.matches(secret, user.passwordHash());
        }
        if (!valid) {
            throw invalidCredentials();
        }
        return UsernamePasswordAuthenticationToken.authenticated(user.id().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Credenciales inválidas");
    }
}
