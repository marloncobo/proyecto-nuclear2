package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.ServicioUsuarios;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@org.springframework.context.annotation.Import(ServicioJwt.class)
public class ConfiguracionIdentidad {
    @Bean
    PasswordEncoder codificadorClaves() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UrlBasedCorsConfigurationSource configuracionCors(
            @Value("${psicoapp.web.origenes-permitidos:}") String origenesConfigurados) {
        var configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(Arrays.stream(origenesConfigurados.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(3600L);
        var fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    @Bean
    SecurityFilterChain seguridadIdentidad(HttpSecurity http, ProveedorAutenticacionIdentidad proveedor,
            ServicioJwt tokens, UrlBasedCorsConfigurationSource fuenteCors) throws Exception {
        return http.authenticationProvider(proveedor)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(fuenteCors))
                .exceptionHandling(errores -> errores.accessDeniedHandler(
                        (solicitud, respuesta, excepcion) -> respuesta.setStatus(403)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(recurso -> recurso.jwt(jwt -> jwt.decoder(tokens::decodificar)
                        .jwtAuthenticationConverter(tokens::autenticar)))
                .authorizeHttpRequests(acceso -> acceso
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/identidad/ingreso",
                                "/api/identidad/recuperacion", "/api/identidad/recuperacion/restablecer").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/identidad/clave-inicial").hasAuthority("CAMBIO_CLAVE_INICIAL")
                        .requestMatchers(HttpMethod.PUT, "/api/identidad/yo/clave")
                                .hasAnyRole("DOCENTE", "ADMINISTRADOR", "ESTUDIANTE")
                        .requestMatchers("/api/identidad/yo").hasAnyAuthority("CAMBIO_CLAVE_INICIAL",
                                "ROLE_DOCENTE", "ROLE_ADMINISTRADOR", "ROLE_ESTUDIANTE")
                        .requestMatchers(HttpMethod.PUT, "/api/identidad/estudiantes/*/clave-inicial")
                                .hasAnyRole("DOCENTE", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/identidad/estudiantes/*")
                                .hasAnyRole("DOCENTE", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/identidad/estudiantes").hasAnyRole("DOCENTE", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/identidad/estudiantes",
                                "/api/identidad/estudiantes/listado").hasAnyRole("DOCENTE", "ADMINISTRADOR")
                        .requestMatchers("/api/identidad/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/academico/**").hasRole("DOCENTE")
                        .requestMatchers("/api/casos", "/api/casos/**").hasRole("DOCENTE")
                        .requestMatchers("/api/sesiones", "/api/sesiones/**").hasRole("DOCENTE")
                        .requestMatchers("/api/notificaciones", "/api/notificaciones/**")
                                .hasAnyRole("DOCENTE", "ESTUDIANTE")
                        .requestMatchers("/api/estudiante", "/api/estudiante/**").hasRole("ESTUDIANTE")
                        .requestMatchers(HttpMethod.POST, "/api/evaluaciones/*/valoraciones").hasRole("DOCENTE")
                        .requestMatchers(HttpMethod.GET, "/api/evaluaciones/*/valoraciones").hasAnyRole("DOCENTE", "ESTUDIANTE")
                        .requestMatchers("/api/evaluaciones/**", "/api/participaciones/**").hasRole("ESTUDIANTE")
                        .requestMatchers(HttpMethod.GET, "/api/reportes/**").hasRole("DOCENTE")
                        .requestMatchers(HttpMethod.POST, "/api/reportes/participaciones/*/valoraciones").hasRole("DOCENTE")
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    ApplicationRunner inicializarAdministrador(ServicioUsuarios identidades,
            @Value("${psicoapp.identidad.correo-administrador}") String correo,
            @Value("${psicoapp.identidad.clave-administrador}") String clave) {
        return argumentos -> {
            if (!correo.isBlank() || !clave.isBlank()) {
                identidades.inicializarPrimerAdministrador(correo, clave);
            }
        };
    }
}
