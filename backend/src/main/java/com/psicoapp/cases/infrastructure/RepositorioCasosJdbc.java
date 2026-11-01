package com.psicoapp.cases.infrastructure;

import com.psicoapp.cases.application.RepositorioCasos;
import com.psicoapp.cases.domain.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Inserta el agregado completo en una transaccion; no publica ni sobrescribe casos. */
@Repository
@Transactional
public class RepositorioCasosJdbc implements RepositorioCasos {
    private final JdbcClient jdbc;

    public RepositorioCasosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(Caso caso) {
        jdbc.sql("""
                INSERT INTO caso (id_caso, id_usuario_docente, nombre, desarrollo_situacional,
                    contexto, tiempo_estimado_min, estado) VALUES (?, ?, ?, ?, ?, ?, ?)
                """).params(java.util.Arrays.asList(caso.id().toString(), caso.idDocente().toString(), caso.nombre(),
                        caso.desarrolloSituacional(), caso.contexto(), caso.tiempoEstimadoMin(), caso.estado())).update();
        for (Escenario escenario : caso.escenarios()) {
            jdbc.sql("""
                    INSERT INTO escenario (id_escenario, id_caso, titulo, contexto_situacional, orden)
                    VALUES (?, ?, ?, ?, ?)
                    """).params(escenario.id().toString(), caso.id().toString(), escenario.titulo(),
                            escenario.contextoSituacional(), escenario.orden()).update();
            for (Pregunta pregunta : escenario.preguntas()) {
                jdbc.sql("INSERT INTO pregunta (id_pregunta, id_escenario, enunciado, orden) VALUES (?, ?, ?, ?)")
                        .params(pregunta.id().toString(), escenario.id().toString(), pregunta.enunciado(), pregunta.orden()).update();
                for (OpcionRespuesta opcion : pregunta.opciones()) {
                    jdbc.sql("""
                            INSERT INTO opcion_respuesta (id_opcion, id_pregunta, texto, es_correcta, justificacion)
                            VALUES (?, ?, ?, ?, ?)
                            """).params(opcion.id().toString(), pregunta.id().toString(), opcion.texto(),
                                    opcion.esCorrecta(), opcion.justificacion()).update();
                }
            }
        }
        if (caso.rubrica() != null) guardarRubrica(caso.id(), caso.rubrica());
    }

    private void guardarRubrica(UUID idCaso, Rubrica rubrica) {
        jdbc.sql("INSERT INTO rubrica (id_rubrica, id_caso, nombre, escala_maxima) VALUES (?, ?, ?, ?)")
                .params(rubrica.id().toString(), idCaso.toString(), rubrica.nombre(), rubrica.escalaMaxima()).update();
        for (CriterioRubrica criterio : rubrica.criterios()) {
            jdbc.sql("INSERT INTO criterio_rubrica (id_criterio, id_rubrica, nombre, ponderacion) VALUES (?, ?, ?, ?)")
                    .params(criterio.id().toString(), rubrica.id().toString(), criterio.nombre(), criterio.ponderacion()).update();
            for (UUID idPregunta : criterio.preguntas()) {
                jdbc.sql("INSERT INTO criterio_pregunta (id_criterio, id_pregunta) VALUES (?, ?)")
                        .params(criterio.id().toString(), idPregunta.toString()).update();
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Caso> buscarPorId(UUID id) {
        return jdbc.sql("SELECT * FROM caso WHERE id_caso = ?").param(id.toString()).query((fila, indice) -> {
            if (!"BORRADOR".equals(fila.getString("estado"))) {
                throw new IllegalArgumentException("Esta entrega solo admite borradores");
            }
            return new Caso(id, uuid(fila, "id_usuario_docente"), fila.getString("nombre"),
                    fila.getString("desarrollo_situacional"), fila.getString("contexto"),
                    fila.getObject("tiempo_estimado_min", Integer.class), cargarEscenarios(id), cargarRubrica(id));
        }).optional();
    }

    private List<Escenario> cargarEscenarios(UUID idCaso) {
        return jdbc.sql("SELECT * FROM escenario WHERE id_caso = ? ORDER BY orden").param(idCaso.toString())
                .query((fila, indice) -> new Escenario(uuid(fila, "id_escenario"), fila.getString("titulo"),
                        fila.getString("contexto_situacional"), fila.getInt("orden"),
                        cargarPreguntas(uuid(fila, "id_escenario")))).list();
    }

    private List<Pregunta> cargarPreguntas(UUID idEscenario) {
        return jdbc.sql("SELECT * FROM pregunta WHERE id_escenario = ? ORDER BY orden").param(idEscenario.toString())
                .query((fila, indice) -> new Pregunta(uuid(fila, "id_pregunta"), fila.getString("enunciado"),
                        fila.getInt("orden"), cargarOpciones(uuid(fila, "id_pregunta")))).list();
    }

    private List<OpcionRespuesta> cargarOpciones(UUID idPregunta) {
        return jdbc.sql("SELECT * FROM opcion_respuesta WHERE id_pregunta = ? ORDER BY id_opcion").param(idPregunta.toString())
                .query((fila, indice) -> new OpcionRespuesta(uuid(fila, "id_opcion"), fila.getString("texto"),
                        fila.getBoolean("es_correcta"), fila.getString("justificacion"))).list();
    }

    private Rubrica cargarRubrica(UUID idCaso) {
        return jdbc.sql("SELECT * FROM rubrica WHERE id_caso = ?").param(idCaso.toString())
                .query((fila, indice) -> new Rubrica(uuid(fila, "id_rubrica"), fila.getString("nombre"),
                        fila.getBigDecimal("escala_maxima"), cargarCriterios(uuid(fila, "id_rubrica")))).optional().orElse(null);
    }

    private List<CriterioRubrica> cargarCriterios(UUID idRubrica) {
        return jdbc.sql("SELECT * FROM criterio_rubrica WHERE id_rubrica = ? ORDER BY id_criterio").param(idRubrica.toString())
                .query((fila, indice) -> new CriterioRubrica(uuid(fila, "id_criterio"), fila.getString("nombre"),
                        fila.getBigDecimal("ponderacion"), jdbc.sql("""
                                SELECT id_pregunta FROM criterio_pregunta WHERE id_criterio = ? ORDER BY id_pregunta
                                """).param(fila.getString("id_criterio")).query((relacion, posicion) ->
                                        uuid(relacion, "id_pregunta")).list())).list();
    }

    private UUID uuid(ResultSet fila, String columna) throws SQLException {
        return UUID.fromString(fila.getString(columna));
    }
}
