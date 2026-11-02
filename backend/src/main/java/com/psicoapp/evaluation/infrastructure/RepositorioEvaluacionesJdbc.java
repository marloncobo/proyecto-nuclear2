package com.psicoapp.evaluation.infrastructure;

import com.psicoapp.evaluation.application.RepositorioEvaluaciones;
import com.psicoapp.evaluation.domain.DetalleEvaluacion;
import com.psicoapp.evaluation.domain.ResultadoEvaluacion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class RepositorioEvaluacionesJdbc implements RepositorioEvaluaciones {
    private final JdbcClient jdbc;

    public RepositorioEvaluacionesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(EvaluacionPersistida evaluacion) {
        jdbc.sql("""
                INSERT INTO evaluacion (id_intento,calificacion,fecha_evaluacion)
                VALUES (?,?,?)
                """).params(evaluacion.idIntento().toString(), evaluacion.resultado().calificacion(),
                Timestamp.from(evaluacion.fecha())).update();
        for (DetalleEvaluacion detalle : evaluacion.resultado().detalles()) {
            jdbc.sql("""
                    INSERT INTO detalle_evaluacion (id_intento,id_pregunta,id_opcion,contestada,correcta,
                        puntos,retroalimentacion)
                    VALUES (?,?,?,?,?,?,?)
                    """).params(evaluacion.idIntento().toString(), detalle.idPregunta().toString(),
                    detalle.idOpcion() == null ? null : detalle.idOpcion().toString(), detalle.contestada(),
                    detalle.correcta(), detalle.puntos(), detalle.retroalimentacion()).update();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EvaluacionPersistida> buscarPorIntento(UUID idIntento) {
        return jdbc.sql("SELECT * FROM evaluacion WHERE id_intento=?")
                .param(idIntento.toString()).query((fila, indice) -> mapear(fila)).optional();
    }

    private EvaluacionPersistida mapear(ResultSet fila) throws SQLException {
        UUID idIntento = UUID.fromString(fila.getString("id_intento"));
        List<DetalleEvaluacion> detalles = jdbc.sql("""
                SELECT id_pregunta,id_opcion,contestada,correcta,puntos,retroalimentacion
                FROM detalle_evaluacion WHERE id_intento=? ORDER BY id_pregunta
                """).param(idIntento.toString()).query((detalle, indice) -> mapearDetalle(detalle, indice)).list();
        return new EvaluacionPersistida(idIntento,
                new ResultadoEvaluacion(fila.getBigDecimal("calificacion"), detalles),
                fila.getTimestamp("fecha_evaluacion").toInstant());
    }

    private DetalleEvaluacion mapearDetalle(ResultSet fila, int indice) throws SQLException {
        String idOpcion = fila.getString("id_opcion");
        return new DetalleEvaluacion(UUID.fromString(fila.getString("id_pregunta")),
                idOpcion == null ? null : UUID.fromString(idOpcion), fila.getBoolean("contestada"),
                fila.getBoolean("correcta"), fila.getBigDecimal("puntos"),
                fila.getString("retroalimentacion"));
    }
}
