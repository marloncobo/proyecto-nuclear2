package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.domain.Usuario;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class ProveedorAutenticacionIdentidad implements AuthenticationProvider {
    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder claves;

    public ProveedorAutenticacionIdentidad(RepositorioUsuarios usuarios, PasswordEncoder claves) {
        this.usuarios = usuarios;
        this.claves = claves;
    }

    @Override
    public Authentication authenticate(Authentication autenticacion) {
        String correo;
        try {
            correo = Usuario.normalizarCorreo(autenticacion.getName());
        } catch (IllegalArgumentException excepcion) {
            throw credencialesInvalidas();
        }
        Usuario usuario = usuarios.buscarPorCorreo(correo).filter(Usuario::activo).orElseThrow(this::credencialesInvalidas);
        String secreto = autenticacion.getCredentials() == null ? "" : autenticacion.getCredentials().toString();
        boolean valido = usuario.hashClave() != null && secreto.getBytes(StandardCharsets.UTF_8).length <= 72
                && claves.matches(secreto, usuario.hashClave());
        if (!valido) {
            throw credencialesInvalidas();
        }
        var identidad = UsernamePasswordAuthenticationToken.authenticated(usuario.id().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
        identidad.setDetails(usuario);
        return identidad;
    }

    @Override
    public boolean supports(Class<?> autenticacion) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(autenticacion);
    }

    private BadCredentialsException credencialesInvalidas() {
        return new BadCredentialsException("Credenciales inválidas");
    }
}
