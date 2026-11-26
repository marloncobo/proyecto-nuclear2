package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.RepositorioUsuarios;
import com.psicoapp.identity.application.FiltroUsuarios;
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
    public void actualizarDatos(UUID id, Rol rolEsperado, String correo, String nombre) {
        if (jdbc.sql("UPDATE usuario SET correo = ?, nombre = ? WHERE id_usuario = ? "
                + "AND id_rol = (SELECT id_rol FROM rol WHERE nombre = ?)")
                .params(correo, nombre, id.toString(), rolEsperado.name()).update() != 1) {
            throw new Conflicto("El rol de la cuenta cambió. Vuelve a consultar sus datos");
        }
    }

    @Override
    public void actualizarEstado(UUID id, boolean activo) {
        jdbc.sql("UPDATE usuario SET activo = ? WHERE id_usuario = ?").params(activo, id.toString()).update();
    }

    @Override
    public List<Usuario> listar(FiltroUsuarios filtro) {
        String patron = "%" + filtro.busqueda().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return jdbc.sql(CONSULTA + "WHERE (:rol IS NULL OR r.nombre = :rol) "
                + "AND (:activo IS NULL OR u.activo = :activo) "
                + "AND (LOWER(u.nombre) LIKE :busqueda ESCAPE '!' OR LOWER(u.correo) LIKE :busqueda ESCAPE '!') "
                + "ORDER BY u.nombre, u.correo, u.id_usuario")
                .param("rol", filtro.rol() == null ? null : filtro.rol().name(), java.sql.Types.VARCHAR)
                .param("activo", filtro.activo(), java.sql.Types.BOOLEAN)
                .param("busqueda", patron).query(this::mapear).list();
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
                    INSERT INTO usuario (id_usuario, correo, nombre, id_rol, contrasena_hash, activo, fecha_creacion, clave_inicial)
                    VALUES (?, ?, ?, (SELECT id_rol FROM rol WHERE nombre = ?), ?, ?, ?, ?)
                    """).param(usuario.id().toString()).param(usuario.correo()).param(usuario.nombre()).param(usuario.rol().name())
                    .param(usuario.hashClave()).param(usuario.activo()).param(Timestamp.from(usuario.fechaCreacion())).param(usuario.claveInicial()).update();
        }
    }

    @Override
    public boolean actualizarClaveEstudiante(UUID id, String hashAnterior, String hashNuevo, boolean inicial) {
        return jdbc.sql("""
                UPDATE usuario SET contrasena_hash = :nuevo, clave_inicial = :inicial
                WHERE id_usuario = :id AND activo = TRUE
                  AND id_rol = (SELECT id_rol FROM rol WHERE nombre = 'ESTUDIANTE')
                  AND ((:anterior IS NULL AND contrasena_hash IS NULL)
                    OR (:anterior IS NOT NULL AND clave_inicial = TRUE AND contrasena_hash = :anterior))
                """).param("nuevo", hashNuevo).param("inicial", inicial)
                .param("id", id.toString()).param("anterior", hashAnterior, java.sql.Types.VARCHAR).update() == 1;
    }

    @Override
    public boolean cambiarClave(UUID id, String hashAnterior, String hashNuevo) {
        return jdbc.sql("""
                UPDATE usuario SET contrasena_hash = ?
                WHERE id_usuario = ? AND activo = TRUE AND clave_inicial = FALSE
                  AND contrasena_hash = ?
                """).params(hashNuevo, id.toString(), hashAnterior).update() == 1;
    }

    private Usuario mapear(ResultSet fila, int indice) throws SQLException {
        return new Usuario(UUID.fromString(fila.getString("id_usuario")), fila.getString("correo"), fila.getString("nombre"),
                Rol.valueOf(fila.getString("rol")), fila.getString("contrasena_hash"), fila.getBoolean("activo"),
                fila.getTimestamp("fecha_creacion").toInstant(), fila.getBoolean("clave_inicial"));
    }
}
