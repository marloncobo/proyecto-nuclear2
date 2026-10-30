package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.UserRepository;
import com.psicoapp.identity.domain.Role;
import com.psicoapp.identity.domain.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserRepository implements UserRepository {
    private static final String SELECT = "SELECT u.*, r.nombre AS rol FROM usuario u JOIN rol r ON r.id_rol = u.id_rol ";
    private final JdbcClient jdbc;

    public JdbcUserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jdbc.sql(SELECT + "WHERE id_usuario = ?").param(id.toString()).query(this::map).optional();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jdbc.sql(SELECT + "WHERE correo = ?").param(email).query(this::map).optional();
    }

    @Override
    public void save(User user) {
        if (findById(user.id()).isPresent()) {
            jdbc.sql("UPDATE usuario SET id_rol = (SELECT id_rol FROM rol WHERE nombre = ?) WHERE id_usuario = ?")
                    .params(user.role().name(), user.id().toString()).update();
        } else {
            jdbc.sql("""
                    INSERT INTO usuario (id_usuario, correo, nombre, id_rol, contrasena_hash, activo, fecha_creacion)
                    VALUES (?, ?, ?, (SELECT id_rol FROM rol WHERE nombre = ?), ?, ?, ?)
                    """).param(user.id().toString()).param(user.email()).param(user.name()).param(user.role().name())
                    .param(user.passwordHash()).param(user.active()).param(Timestamp.from(user.createdAt())).update();
        }
    }

    private User map(ResultSet row, int index) throws SQLException {
        return new User(UUID.fromString(row.getString("id_usuario")), row.getString("correo"), row.getString("nombre"),
                Role.valueOf(row.getString("rol")), row.getString("contrasena_hash"), row.getBoolean("activo"),
                row.getTimestamp("fecha_creacion").toInstant());
    }
}