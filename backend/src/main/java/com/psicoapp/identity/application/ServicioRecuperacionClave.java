package com.psicoapp.identity.application;

import com.psicoapp.identity.domain.Usuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioRecuperacionClave {
    private static final Duration VIGENCIA = Duration.ofMinutes(30);
    private static final Duration ESPERA = Duration.ofMinutes(2);
    private final RepositorioUsuarios usuarios;
    private final RepositorioRecuperacionesClave recuperaciones;
    private final NotificadorRecuperacionClave notificador;
    private final PasswordEncoder claves;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    public ServicioRecuperacionClave(RepositorioUsuarios usuarios, RepositorioRecuperacionesClave recuperaciones,
            NotificadorRecuperacionClave notificador, PasswordEncoder claves, Clock reloj) {
        this.usuarios = usuarios;
        this.recuperaciones = recuperaciones;
        this.notificador = notificador;
        this.claves = claves;
        this.reloj = reloj;
    }

    public void solicitar(String correo) {
        String normalizado = Usuario.normalizarCorreo(correo);
        var encontrada = usuarios.buscarPorCorreo(normalizado);
        if (encontrada.isEmpty()) return;
        var cuenta = usuarios.bloquear(encontrada.get().id())
                .filter(usuario -> usuario.activo() && usuario.correo().equals(normalizado));
        if (cuenta.isEmpty()) return;
        Usuario usuario = cuenta.get();
        var ahora = reloj.instant().truncatedTo(ChronoUnit.SECONDS);
        if (recuperaciones.buscar(usuario.id())
                .filter(solicitud -> ahora.isBefore(solicitud.vence().minus(VIGENCIA).plus(ESPERA))).isPresent()) return;

        byte[] secreto = new byte[32];
        aleatorio.nextBytes(secreto);
        String token = usuario.id() + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(secreto);
        var vence = ahora.plus(VIGENCIA);
        recuperaciones.guardar(usuario.id(), new RepositorioRecuperacionesClave.Recuperacion(resumir(token, usuario), vence));
        notificador.encolar(usuario.id(), usuario.correo(), token, vence);
    }

    public void restablecer(String token, String clave) {
        ServicioUsuarios.validarClave(clave);
        if (token == null || token.length() != 80 || token.charAt(36) != '.') throw enlaceInvalido();
        UUID id;
        try {
            id = UUID.fromString(token.substring(0, 36));
        } catch (IllegalArgumentException excepcion) {
            throw enlaceInvalido();
        }
        Usuario usuario = usuarios.bloquear(id).filter(Usuario::activo).orElseThrow(this::enlaceInvalido);
        var solicitud = recuperaciones.buscar(id).orElseThrow(this::enlaceInvalido);
        if (!reloj.instant().isBefore(solicitud.vence())
                || !MessageDigest.isEqual(solicitud.resumen().getBytes(StandardCharsets.US_ASCII),
                        resumir(token, usuario).getBytes(StandardCharsets.US_ASCII))) {
            throw enlaceInvalido();
        }
        usuarios.restablecerClave(id, claves.encode(clave));
        recuperaciones.eliminar(id);
    }

    private static String resumir(String token, Usuario usuario) {
        try {
            String datos = token + "\n" + usuario.correo() + "\n" + usuario.hashClave();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(datos.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException excepcion) {
            throw new IllegalStateException("SHA-256 no está disponible", excepcion);
        }
    }

    private IllegalArgumentException enlaceInvalido() {
        return new IllegalArgumentException("El enlace ya no es válido. Solicita uno nuevo.");
    }
}
