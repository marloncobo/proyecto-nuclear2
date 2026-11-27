package com.psicoapp.identity.api;

import com.psicoapp.identity.application.ServicioRecuperacionClave;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/identidad/recuperacion")
public class ControladorRecuperacionClave {
    private static final Logger REGISTRO = LoggerFactory.getLogger(ControladorRecuperacionClave.class);
    public record Solicitud(@NotBlank @Email @Size(max = 180) String correo) { }
    public record Restablecimiento(@NotBlank @Pattern(regexp =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[A-Za-z0-9_-]{43}") String token,
            @NotBlank @Size(min = 8, max = 72) String clave) {
        @Override public String toString() { return "Restablecimiento[datos protegidos]"; }
    }

    private final ServicioRecuperacionClave recuperacion;

    public ControladorRecuperacionClave(ServicioRecuperacionClave recuperacion) {
        this.recuperacion = recuperacion;
    }

    @PostMapping
    public ResponseEntity<Void> solicitar(@Valid @RequestBody Solicitud solicitud) {
        try {
            recuperacion.solicitar(solicitud.correo());
        } catch (DataAccessException | IllegalStateException excepcion) {
            // Mantener la respuesta uniforme sin registrar el destinatario ni el enlace.
            REGISTRO.error("No se pudo preparar un correo de recuperación");
        }
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/restablecer")
    public ResponseEntity<Void> restablecer(@Valid @RequestBody Restablecimiento solicitud) {
        recuperacion.restablecer(solicitud.token(), solicitud.clave());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
