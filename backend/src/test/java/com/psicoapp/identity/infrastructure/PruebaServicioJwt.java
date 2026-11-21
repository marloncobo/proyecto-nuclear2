package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.application.VerificadorAccesoEstudiante;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RF-02: validacion criptografica y temporal JWT")
class PruebaServicioJwt {
    // Material exclusivamente ficticio para pruebas, sin uso en la configuracion de la aplicacion.
    private final byte[] clave = new byte[32];
    private final RepositorioUsuarios usuarios = mock(RepositorioUsuarios.class);
    private final Environment entorno = mock(Environment.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<VerificadorAccesoEstudiante> convocatorias = mock(ObjectProvider.class);

    @Test
    void rechazaVencidosEmisorAudienciaIncorrectosYTokensSinExpiracion() {
        var servicio = servicio();
        assertThrows(JwtException.class, () -> servicio.decodificar(firmar("psicoapp", "psicoapp", Instant.now().minusSeconds(10))));
        assertThrows(JwtException.class, () -> servicio.decodificar(firmar("otro", "psicoapp", Instant.now().plusSeconds(60))));
        assertThrows(JwtException.class, () -> servicio.decodificar(firmar("psicoapp", "otro", Instant.now().plusSeconds(60))));
        assertThrows(JwtException.class, () -> servicio.decodificar(firmar("psicoapp", "psicoapp", null)));
        assertNotNull(servicio.decodificar(firmar("psicoapp", "psicoapp", Instant.now().plusSeconds(60))));
    }

    @Test
    void rechazaConfiguracionesDebiles() {
        when(entorno.getActiveProfiles()).thenReturn(new String[0]);
        assertThrows(IllegalArgumentException.class, () -> new ServicioJwt(entorno, usuarios, convocatorias, "no-base64!", 900, false));
        assertThrows(IllegalArgumentException.class, () -> new ServicioJwt(entorno, usuarios, convocatorias, "YQ==", 900, false));
        assertThrows(IllegalArgumentException.class, () -> new ServicioJwt(entorno, usuarios, convocatorias, "", 0, false));
        assertThrows(IllegalArgumentException.class, () -> new ServicioJwt(entorno, usuarios, convocatorias, "", 3601, false));
        assertThrows(IllegalArgumentException.class, () -> new ServicioJwt(entorno, usuarios, convocatorias, "", 900, true));
        when(entorno.getActiveProfiles()).thenReturn(new String[] {"prod"});
        assertThrows(IllegalArgumentException.class, () -> new ServicioJwt(entorno, usuarios, convocatorias, "", 900, false));
    }

    @Test
    void claveEfimeraNoAceptaTokensDeOtraInstancia() {
        when(entorno.getActiveProfiles()).thenReturn(new String[0]);
        var servicio = new ServicioJwt(entorno, usuarios, convocatorias, "", 900, false);
        assertThrows(JwtException.class, () -> servicio.decodificar(firmar("psicoapp", "psicoapp", Instant.now().plusSeconds(60))));
    }

    private ServicioJwt servicio() {
        when(entorno.getActiveProfiles()).thenReturn(new String[0]);
        return new ServicioJwt(entorno, usuarios, convocatorias, Base64.getEncoder().encodeToString(clave), 900, true);
    }

    private String firmar(String emisor, String audiencia, Instant vencimiento) {
        var datos = JwtClaimsSet.builder().issuer(emisor).audience(List.of(audiencia))
                .subject(UUID.randomUUID().toString()).issuedAt(Instant.now().minusSeconds(120));
        if (vencimiento != null) datos.expiresAt(vencimiento);
        return NimbusJwtEncoder.withSecretKey(new SecretKeySpec(clave, "HmacSHA256"))
                .algorithm(MacAlgorithm.HS256).build().encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), datos.build())).getTokenValue();
    }
}
