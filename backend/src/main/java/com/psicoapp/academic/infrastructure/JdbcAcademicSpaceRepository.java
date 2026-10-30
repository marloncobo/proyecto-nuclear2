package com.psicoapp.academic.infrastructure;

import com.psicoapp.academic.application.AcademicSpaceRepository;
import com.psicoapp.academic.domain.AcademicSpace;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAcademicSpaceRepository implements AcademicSpaceRepository {
    private final JdbcClient jdbc;

    public JdbcAcademicSpaceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(AcademicSpace space) {
        jdbc.sql("INSERT INTO espacio_academico (id_espacio, nombre, periodo) VALUES (?, ?, ?)")
                .params(space.id().toString(), space.name(), space.period()).update();
    }

    @Override
    public Optional<AcademicSpace> findById(UUID id) {
        return jdbc.sql("SELECT * FROM espacio_academico WHERE id_espacio = ?")
                .param(id.toString()).query(this::map).optional();
    }

    @Override
    public List<AcademicSpace> findAll() {
        return jdbc.sql("SELECT * FROM espacio_academico ORDER BY periodo, nombre, id_espacio")
                .query(this::map).list();
    }

    private AcademicSpace map(ResultSet row, int index) throws SQLException {
        return new AcademicSpace(UUID.fromString(row.getString("id_espacio")), row.getString("nombre"),
                row.getString("periodo"));
    }
}