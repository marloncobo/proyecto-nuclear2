package com.psicoapp.academic.infrastructure;

import com.psicoapp.academic.application.RepositorioEspaciosAcademicos;
import com.psicoapp.academic.domain.EspacioAcademico;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Repository
public class RepositorioEspaciosAcademicosJdbc implements RepositorioEspaciosAcademicos {
    private final JdbcClient jdbc;

    public RepositorioEspaciosAcademicosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<EspacioAcademico> bloquearPorId(UUID id) {
        return jdbc.sql("SELECT * FROM espacio_academico WHERE id_espacio = ? FOR UPDATE")
                .param(id.toString()).query(this::mapear).optional();
    }

    @Override
    public void actualizar(EspacioAcademico espacio) {
        jdbc.sql("UPDATE espacio_academico SET nombre = ?, periodo = ? WHERE id_espacio = ?")
                .params(espacio.nombre(), espacio.periodo(), espacio.id().toString()).update();
    }

    @Override
    public void guardar(EspacioAcademico espacio) {
        jdbc.sql("INSERT INTO espacio_academico (id_espacio, nombre, periodo) VALUES (?, ?, ?)")
                .params(espacio.id().toString(), espacio.nombre(), espacio.periodo()).update();
    }

    @Override
    public Optional<EspacioAcademico> buscarPorId(UUID id) {
        return jdbc.sql("SELECT * FROM espacio_academico WHERE id_espacio = ?")
                .param(id.toString()).query(this::mapear).optional();
    }

    @Override
    public List<EspacioAcademico> listarTodos() {
        return jdbc.sql("SELECT * FROM espacio_academico ORDER BY periodo, nombre, id_espacio")
                .query(this::mapear).list();
    }

    private EspacioAcademico mapear(ResultSet fila, int indice) throws SQLException {
        return new EspacioAcademico(UUID.fromString(fila.getString("id_espacio")), fila.getString("nombre"),
                fila.getString("periodo"));
    }
}
