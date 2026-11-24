package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.application.VerificadorAccesoEstudiante;
import com.psicoapp.identity.domain.Rol;
import com.psicoapp.identity.domain.Usuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Firma y valida tokens; la cuenta y convocatoria se consultan en cada solicitud. */
public class ServicioJwt {
    private static final String EMISOR = "psicoapp";
    private final NimbusJwtEncoder codificador;
    private final NimbusJwtDecoder decodificador;
    private final RepositorioUsuarios usuarios;
    private final ObjectProvider<VerificadorAccesoEstudiante> accesoEstudiantil;
    private final Duration duracion;

    public ServicioJwt(Environment entorno, RepositorioUsuarios usuarios,
            ObjectProvider<VerificadorAccesoEstudiante> accesoEstudiantil,
            @Value("${psicoapp.identidad.jwt.clave:${PSICOAPP_JWT_CLAVE:}}") String clave,
            @Value("${psicoapp.identidad.jwt.duracion-segundos:900}") long duracionSegundos,
            @Value("${psicoapp.identidad.jwt.clave-requerida:false}") boolean claveRequerida) {
        this.usuarios = usuarios;
        this.accesoEstudiantil = accesoEstudiantil;
        if (duracionSegundos < 1 || duracionSegundos > 3600)
            throw new IllegalArgumentException("La duracion JWT debe estar entre 1 y 3600 segundos");
        duracion = Duration.ofSeconds(duracionSegundos);
        byte[] bytes;
        boolean perfilProduccion = java.util.Arrays.stream(entorno.getActiveProfiles())
                .anyMatch(perfil -> perfil.equalsIgnoreCase("prod") || perfil.equalsIgnoreCase("production"));
        if (clave.isBlank()) {
            if (claveRequerida || perfilProduccion) {
                throw new IllegalArgumentException("La clave JWT persistente es obligatoria en este entorno");
            }
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            try {
                bytes = Base64.getDecoder().decode(clave);
            } catch (IllegalArgumentException excepcion) {
                throw new IllegalArgumentException("La clave JWT debe estar codificada en Base64");
            }
            if (bytes.length < 32) throw new IllegalArgumentException("La clave JWT requiere al menos 32 bytes");
        }
        var secreto = new SecretKeySpec(bytes, "HmacSHA256");
        codificador = NimbusJwtEncoder.withSecretKey(secreto).algorithm(MacAlgorithm.HS256).build();
        decodificador = NimbusJwtDecoder.withSecretKey(secreto).macAlgorithm(MacAlgorithm.HS256).build();
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO), new JwtIssuerValidator(EMISOR),
                token -> token.getExpiresAt() != null && token.getIssuedAt() != null
                        && token.getAudience().contains(EMISOR)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"))));
    }

    public Jwt emitir(Authentication identidad, String credencial) {
        var usuario = usuarios.buscarPorId(UUID.fromString(identidad.getName()))
                .filter(Usuario::activo).orElseThrow(this::invalido);
        Instant ahora = Instant.now();
        var datos = JwtClaimsSet.builder().issuer(EMISOR).audience(List.of(EMISOR))
                .subject(usuario.id().toString()).issuedAt(ahora).expiresAt(ahora.plus(duracion))
                .id(UUID.randomUUID().toString()).claim("rol", usuario.rol().name());
        if (usuario.rol() == Rol.ESTUDIANTE) {
            if (!(identidad.getDetails() instanceof Usuario autenticado)
                    || !java.util.Objects.equals(autenticado.hashClave(), usuario.hashClave())) throw invalido();
            datos.claim("claveInicial", usuario.claveInicial());
        }
        return codificador.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), datos.build()));
    }

    public Jwt decodificar(String token) {
        return decodificador.decode(token);
    }

    public AbstractAuthenticationToken autenticar(Jwt token) {
        UUID id;
        try {
            id = UUID.fromString(token.getSubject());
        } catch (IllegalArgumentException | NullPointerException excepcion) {
            throw invalido();
        }
        var usuario = usuarios.buscarPorId(id).filter(Usuario::activo).orElseThrow(this::invalido);
        if (!usuario.rol().name().equals(token.getClaimAsString("rol"))) throw invalido();
        String autoridad = "ROLE_" + usuario.rol().name();
        if (usuario.rol() == Rol.ESTUDIANTE) {
            if (usuario.hashClave() == null || !Boolean.valueOf(usuario.claveInicial())
                    .equals(token.getClaimAsBoolean("claveInicial"))) throw invalido();
            if (usuario.claveInicial()) autoridad = "CAMBIO_CLAVE_INICIAL";
        }
        return new JwtAuthenticationToken(token,
                List.of(new SimpleGrantedAuthority(autoridad)), id.toString());
    }

    private InvalidBearerTokenException invalido() {
        return new InvalidBearerTokenException("Token invalido");
    }

}
