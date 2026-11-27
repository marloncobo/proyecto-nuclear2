package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.RepositorioRecuperacionesClave;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioRecuperacionesClaveJdbc implements RepositorioRecuperacionesClave {
    private final JdbcClient jdbc;

    public RepositorioRecuperacionesClaveJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override public Optional<Recuperacion> buscar(UUID usuario) {
        return jdbc.sql("SELECT resumen, vence FROM recuperacion_clave WHERE id_usuario = ? FOR UPDATE")
                .param(usuario.toString()).query((fila, indice) ->
                        new Recuperacion(fila.getString("resumen"), fila.getTimestamp("vence").toInstant())).optional();
    }

    @Override public void guardar(UUID usuario, Recuperacion recuperacion) {
        jdbc.sql("""
                INSERT INTO recuperacion_clave (id_usuario, resumen, vence) VALUES (:id, :resumen, :vence)
                ON DUPLICATE KEY UPDATE resumen = :resumen, vence = :vence
                """).param("id", usuario.toString()).param("resumen", recuperacion.resumen())
                .param("vence", Timestamp.from(recuperacion.vence())).update();
    }

    @Override public void eliminar(UUID usuario) {
        jdbc.sql("DELETE FROM recuperacion_clave WHERE id_usuario = ?").param(usuario.toString()).update();
    }
}
