package com.psicoapp.sessions.infrastructure;

import com.psicoapp.academic.application.ConsultaUsoEspacios;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ConsultaUsoEspaciosJdbc implements ConsultaUsoEspacios {
    private final JdbcClient jdbc;

    public ConsultaUsoEspaciosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean tieneSesiones(UUID idEspacio) {
        // Lectura actual tambien con el aislamiento REPEATABLE READ de MySQL.
        return !jdbc.sql("SELECT id_sesion FROM sesion WHERE id_espacio = ? LIMIT 1 FOR UPDATE")
                .param(idEspacio.toString()).query(String.class).list().isEmpty();
    }
}
