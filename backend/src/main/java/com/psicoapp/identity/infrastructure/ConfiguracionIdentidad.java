package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.ServicioUsuarios;
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
public class ConfiguracionIdentidad {
    @Bean
    PasswordEncoder codificadorClaves() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain seguridadIdentidad(HttpSecurity http, ProveedorAutenticacionIdentidad proveedor) throws Exception {
        return http.authenticationProvider(proveedor)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .exceptionHandling(errores -> errores.accessDeniedHandler(
                        (solicitud, respuesta, excepcion) -> respuesta.setStatus(403)))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(acceso -> acceso
                        .requestMatchers("/api/identidad/yo", "/api/identidad/csrf").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/identidad/estudiantes").hasRole("DOCENTE")
                        .requestMatchers("/api/identidad/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/academico/**").hasRole("DOCENTE")
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    ApplicationRunner inicializarAdministrador(ServicioUsuarios identidades,
            @Value("${psicoapp.identidad.correo-administrador}") String correo,
            @Value("${psicoapp.identidad.clave-administrador}") String clave) {
        return argumentos -> {
            if (!correo.isBlank() || !clave.isBlank()) {
                identidades.inicializarAdministrador(correo, clave);
            }
        };
    }
}
