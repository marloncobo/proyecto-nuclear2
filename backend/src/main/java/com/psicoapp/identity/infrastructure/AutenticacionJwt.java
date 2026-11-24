package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.Autenticacion;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionJwt implements Autenticacion {
    private final ProveedorAutenticacionIdentidad proveedor;
    private final ServicioJwt tokens;

    public AutenticacionJwt(ProveedorAutenticacionIdentidad proveedor, ServicioJwt tokens) {
        this.proveedor = proveedor;
        this.tokens = tokens;
    }

    @Override
    public DatosToken ingresar(String correo, String clave) {
        var identidad = proveedor.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(correo, clave));
        var token = tokens.emitir(identidad, clave);
        return new DatosToken(token.getTokenValue(), "Bearer", token.getExpiresAt(), Boolean.TRUE.equals(token.getClaimAsBoolean("claveInicial")));
    }
}
