package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.domain.Rol;
import com.psicoapp.identity.domain.Usuario;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioUsuariosJdbc implements RepositorioUsuarios {
    private static final String CONSULTA = "SELECT u.*, r.nombre AS rol FROM usuario u JOIN rol r ON r.id_rol = u.id_rol ";
    private final JdbcClient jdbc;

    public RepositorioUsuariosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return jdbc.sql(CONSULTA + "WHERE id_usuario = ?").param(id.toString()).query(this::mapear).optional();
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return jdbc.sql(CONSULTA + "WHERE correo = ?").param(correo).query(this::mapear).optional();
    }

    @Override
    public void guardar(Usuario usuario) {
        if (buscarPorId(usuario.id()).isPresent()) {
            jdbc.sql("UPDATE usuario SET id_rol = (SELECT id_rol FROM rol WHERE nombre = ?) WHERE id_usuario = ?")
                    .params(usuario.rol().name(), usuario.id().toString()).update();
        } else {
            jdbc.sql("""
                    INSERT INTO usuario (id_usuario, correo, nombre, id_rol, contrasena_hash, activo, fecha_creacion)
                    VALUES (?, ?, ?, (SELECT id_rol FROM rol WHERE nombre = ?), ?, ?, ?)
                    """).param(usuario.id().toString()).param(usuario.correo()).param(usuario.nombre()).param(usuario.rol().name())
                    .param(usuario.hashClave()).param(usuario.activo()).param(Timestamp.from(usuario.fechaCreacion())).update();
        }
    }

    private Usuario mapear(ResultSet fila, int indice) throws SQLException {
        return new Usuario(UUID.fromString(fila.getString("id_usuario")), fila.getString("correo"), fila.getString("nombre"),
                Rol.valueOf(fila.getString("rol")), fila.getString("contrasena_hash"), fila.getBoolean("activo"),
                fila.getTimestamp("fecha_creacion").toInstant());
    }
}
