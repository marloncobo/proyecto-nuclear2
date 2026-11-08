package com.psicoapp.evaluation.infrastructure;

import com.psicoapp.evaluation.application.ProveedorCasosEvaluables;
import com.psicoapp.evaluation.domain.CriterioEvaluable;
import com.psicoapp.evaluation.domain.OpcionEvaluable;
import com.psicoapp.evaluation.domain.PreguntaEvaluable;
import com.psicoapp.evaluation.domain.RubricaEvaluable;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ProveedorCasosEvaluablesJdbc implements ProveedorCasosEvaluables {
    private final JdbcClient jdbc;

    public ProveedorCasosEvaluablesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CasoEvaluable buscarPorSesion(UUID idSesion) {
        UUID idCaso = jdbc.sql("SELECT id_caso FROM sesion WHERE id_sesion=?")
                .param(idSesion.toString()).query(String.class).optional()
                .map(UUID::fromString).orElseThrow(() -> new NoEncontrado("Sesion no encontrada"));
        List<PreguntaEvaluable> preguntas = jdbc.sql("""
                SELECT p.id_pregunta
                FROM pregunta p
                JOIN escenario e ON e.id_escenario=p.id_escenario
                WHERE e.id_caso=?
                ORDER BY e.orden,p.orden
                """).param(idCaso.toString()).query((fila, indice) -> mapearPregunta(fila)).list();
        RubricaEvaluable rubrica = jdbc.sql("""
                SELECT id_rubrica,escala_maxima FROM rubrica WHERE id_caso=?
                """).param(idCaso.toString()).query((fila, indice) -> mapearRubrica(fila)).optional()
                .orElseThrow(() -> new NoEncontrado("El caso no tiene rubrica"));
        return new CasoEvaluable(preguntas, rubrica);
    }

    private PreguntaEvaluable mapearPregunta(ResultSet fila) throws SQLException {
        UUID idPregunta = UUID.fromString(fila.getString("id_pregunta"));
        List<OpcionEvaluable> opciones = jdbc.sql("""
                SELECT id_opcion,es_correcta,justificacion
                FROM opcion_respuesta WHERE id_pregunta=? ORDER BY id_opcion
                """).param(idPregunta.toString()).query(this::mapearOpcion).list();
        return new PreguntaEvaluable(idPregunta, opciones);
    }

    private OpcionEvaluable mapearOpcion(ResultSet fila, int indice) throws SQLException {
        return new OpcionEvaluable(UUID.fromString(fila.getString("id_opcion")),
                fila.getBoolean("es_correcta"), fila.getString("justificacion"));
    }

    private RubricaEvaluable mapearRubrica(ResultSet fila) throws SQLException {
        UUID idRubrica = UUID.fromString(fila.getString("id_rubrica"));
        List<CriterioEvaluable> criterios = jdbc.sql("""
                SELECT id_criterio,ponderacion FROM criterio_rubrica WHERE id_rubrica=? ORDER BY id_criterio
                """).param(idRubrica.toString()).query((criterio, indice) -> mapearCriterio(criterio)).list();
        return new RubricaEvaluable(fila.getBigDecimal("escala_maxima"), criterios);
    }

    private CriterioEvaluable mapearCriterio(ResultSet fila) throws SQLException {
        UUID idCriterio = UUID.fromString(fila.getString("id_criterio"));
        List<UUID> preguntas = jdbc.sql("""
                SELECT id_pregunta FROM criterio_pregunta WHERE id_criterio=? ORDER BY id_pregunta
                """).param(idCriterio.toString()).query((pregunta, indice) ->
                UUID.fromString(pregunta.getString("id_pregunta"))).list();
        return new CriterioEvaluable(fila.getBigDecimal("ponderacion"), preguntas);
    }
}
