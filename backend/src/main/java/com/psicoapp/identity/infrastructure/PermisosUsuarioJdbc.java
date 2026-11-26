package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.PermisosUsuario;
import com.psicoapp.shared.exceptions.Conflicto;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PermisosUsuarioJdbc implements PermisosUsuario {
    private final JdbcClient jdbc;

    public PermisosUsuarioJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean puedeCrearCasos(UUID idUsuario) {
        return jdbc.sql("SELECT COUNT(*) FROM permiso_creacion_casos WHERE id_usuario = ?")
                .param(idUsuario.toString()).query(Integer.class).single() > 0;
    }

    @Override
    public void configurarCreacionCasos(UUID idUsuario, boolean habilitado) {
        // Serializa la concesión con cambios de rol de la misma cuenta.
        var docente = jdbc.sql("""
                SELECT id_usuario FROM usuario
                WHERE id_usuario = ? AND id_rol = (SELECT id_rol FROM rol WHERE nombre = 'DOCENTE')
                FOR UPDATE
                """).param(idUsuario.toString()).query(String.class).optional();
        if (docente.isEmpty()) throw new Conflicto("La cuenta ya no tiene rol docente");
        if (habilitado) {
            var permiso = jdbc.sql("SELECT id_usuario FROM permiso_creacion_casos WHERE id_usuario = ? FOR UPDATE")
                    .param(idUsuario.toString()).query(String.class).optional();
            if (permiso.isEmpty()) {
                jdbc.sql("INSERT INTO permiso_creacion_casos (id_usuario) VALUES (?)")
                        .param(idUsuario.toString()).update();
            }
        } else {
            retirarCreacionCasos(idUsuario);
        }
    }

    @Override
    public void retirarCreacionCasos(UUID idUsuario) {
        jdbc.sql("DELETE FROM permiso_creacion_casos WHERE id_usuario = ?").param(idUsuario.toString()).update();
    }
}
