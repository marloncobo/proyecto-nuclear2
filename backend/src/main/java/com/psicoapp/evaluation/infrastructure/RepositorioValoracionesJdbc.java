package com.psicoapp.evaluation.infrastructure;

import com.psicoapp.evaluation.application.RepositorioValoraciones;
import java.util.*;
import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioValoracionesJdbc implements RepositorioValoraciones {
    private final JdbcClient jdbc;
    public RepositorioValoracionesJdbc(JdbcClient jdbc) { this.jdbc = jdbc; }
    @Override public void guardar(Valoracion valoracion) {
        jdbc.sql("INSERT INTO valoracion_docente (id_valoracion,id_intento,id_docente,mensaje,fecha_creacion) VALUES (?,?,?,?,?)")
                .params(valoracion.id().toString(), valoracion.idIntento().toString(), valoracion.idDocente().toString(),
                        valoracion.mensaje(), Timestamp.from(valoracion.fecha())).update();
    }
    @Override public List<Valoracion> listar(UUID intento) {
        return jdbc.sql("SELECT * FROM valoracion_docente WHERE id_intento=? ORDER BY fecha_creacion,id_valoracion")
                .param(intento.toString()).query((fila, indice) -> new Valoracion(UUID.fromString(fila.getString("id_valoracion")),
                    intento, UUID.fromString(fila.getString("id_docente")), fila.getString("mensaje"),
                    fila.getTimestamp("fecha_creacion").toInstant())).list();
    }
}
