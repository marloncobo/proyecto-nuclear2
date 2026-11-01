package com.psicoapp.sessions.infrastructure;

import com.psicoapp.sessions.application.RepositorioConvocatorias;
import com.psicoapp.sessions.domain.Convocatoria;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioConvocatoriasJdbc implements RepositorioConvocatorias {
    private final JdbcClient jdbc;
    public RepositorioConvocatoriasJdbc(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public void guardar(Convocatoria convocatoria) {
        jdbc.sql("INSERT INTO convocatoria (id_convocatoria,id_sesion,id_estudiante,codigo_acceso,estado) VALUES (?,?,?,?,?)")
                .params(convocatoria.id().toString(), convocatoria.idSesion().toString(),
                        convocatoria.idEstudiante().toString(), convocatoria.resumenCodigo(),
                        convocatoria.autorizada() ? "AUTORIZADA" : "RETIRADA").update();
    }

    @Override
    public List<Convocatoria> listarPorSesion(UUID idSesion) {
        return jdbc.sql("SELECT * FROM convocatoria WHERE id_sesion=? ORDER BY id_estudiante")
                .param(idSesion.toString()).query(this::mapear).list();
    }

    @Override
    public Optional<Convocatoria> buscarAutorizada(UUID idEstudiante, String resumenCodigo) {
        return jdbc.sql("SELECT * FROM convocatoria WHERE id_estudiante=? AND codigo_acceso=? AND estado='AUTORIZADA'")
                .params(idEstudiante.toString(), resumenCodigo).query(this::mapear).optional();
    }

    private Convocatoria mapear(ResultSet fila, int indice) throws SQLException {
        return new Convocatoria(UUID.fromString(fila.getString("id_convocatoria")),
                UUID.fromString(fila.getString("id_sesion")), UUID.fromString(fila.getString("id_estudiante")),
                fila.getString("codigo_acceso"), "AUTORIZADA".equals(fila.getString("estado")));
    }
}
