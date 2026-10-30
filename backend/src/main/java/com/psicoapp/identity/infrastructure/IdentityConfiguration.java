package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.IdentityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class IdentityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain identitySecurity(HttpSecurity http, IdentityAuthenticationProvider provider) throws Exception {
        return http.authenticationProvider(provider)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .exceptionHandling(errors -> errors.accessDeniedHandler(
                        (request, response, error) -> response.setStatus(403)))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(access -> access
                        .requestMatchers("/api/identity/me", "/api/identity/csrf").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/identity/students").hasRole("DOCENTE")
                        .requestMatchers("/api/identity/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/academic/**").hasRole("DOCENTE")
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    ApplicationRunner initializeAdministrator(IdentityService identities,
            @Value("${psicoapp.identity.admin-email}") String email,
            @Value("${psicoapp.identity.admin-password}") String password) {
        return args -> {
            if (!email.isBlank() || !password.isBlank()) {
                identities.initializeAdministrator(email, password);
            }
        };
    }
}
