package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.domain.Rol;
import com.psicoapp.identity.domain.Usuario;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.List;
import com.psicoapp.shared.exceptions.Conflicto;
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
    public void actualizarDatosDocente(UUID id, String correo, String nombre) {
        if (jdbc.sql("UPDATE usuario SET correo = ?, nombre = ? WHERE id_usuario = ? "
                + "AND id_rol = (SELECT id_rol FROM rol WHERE nombre = 'DOCENTE')")
                .params(correo, nombre, id.toString()).update() != 1) {
            throw new Conflicto("La cuenta ya no tiene rol docente");
        }
    }

    @Override
    public void actualizarEstado(UUID id, boolean activo) {
        jdbc.sql("UPDATE usuario SET activo = ? WHERE id_usuario = ?").params(activo, id.toString()).update();
    }

    @Override
    public List<Usuario> listar(Rol rol, Boolean activo) {
        return jdbc.sql(CONSULTA + "WHERE (:rol IS NULL OR r.nombre = :rol) "
                + "AND (:activo IS NULL OR u.activo = :activo) ORDER BY u.nombre, u.correo, u.id_usuario")
                .param("rol", rol == null ? null : rol.name(), java.sql.Types.VARCHAR)
                .param("activo", activo, java.sql.Types.BOOLEAN).query(this::mapear).list();
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
