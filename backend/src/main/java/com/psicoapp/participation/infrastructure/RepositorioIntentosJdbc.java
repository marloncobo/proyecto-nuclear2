package com.psicoapp.participation.infrastructure;

import com.psicoapp.participation.application.RepositorioIntentos;
import com.psicoapp.participation.application.RepositorioConsultaIntentos;
import com.psicoapp.participation.domain.EstadoIntento;
import com.psicoapp.participation.domain.Intento;
import com.psicoapp.sessions.domain.ModoNavegacion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class RepositorioIntentosJdbc implements RepositorioIntentos, RepositorioConsultaIntentos {
    private final JdbcClient jdbc;

    public RepositorioIntentosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(Intento intento) {
        jdbc.sql("""
                INSERT INTO intento (id_intento,id_sesion,id_estudiante,fecha_inicio,fecha_limite,
                    posicion_actual,modo_navegacion,estado)
                VALUES (?,?,?,?,?,?,?,?)
                """).params(java.util.Arrays.asList(intento.id().toString(), intento.idSesion().toString(),
                intento.idEstudiante().toString(), Timestamp.from(intento.inicio()), Timestamp.from(intento.limite()),
                intento.posicionActual(), intento.modoNavegacion().name(), intento.estado().name())).update();
        int orden = 0;
        for (UUID idPregunta : intento.preguntas()) {
            jdbc.sql("INSERT INTO intento_pregunta (id_intento,id_pregunta,orden) VALUES (?,?,?)")
                    .params(intento.id().toString(), idPregunta.toString(), orden++).update();
        }
        guardarRespuestas(intento);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Intento> buscarPorId(UUID id) {
        return buscar("SELECT * FROM intento WHERE id_intento = ?", id.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Intento> buscarActivo(UUID idSesion, UUID idEstudiante) {
        return jdbc.sql("""
                SELECT * FROM intento
                WHERE id_sesion=? AND id_estudiante=? AND estado='EN_PROGRESO'
                ORDER BY fecha_inicio DESC
                """).params(idSesion.toString(), idEstudiante.toString()).query(this::mapear).optional();
    }

    @Override
    public void actualizar(Intento intento) {
        if (jdbc.sql("""
                UPDATE intento SET posicion_actual=?, estado=?, fecha_limite=?
                WHERE id_intento=?
                """).params(intento.posicionActual(), intento.estado().name(), Timestamp.from(intento.limite()),
                intento.id().toString()).update() != 1) {
            throw new IllegalStateException("El intento no existe");
        }
        guardarRespuestas(intento);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Optional<Intento> bloquearPorId(UUID id) {
        return buscar("SELECT * FROM intento WHERE id_intento = ? FOR UPDATE", id.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Intento> buscarVencidos(Instant ahora) {
        return jdbc.sql("""
                SELECT * FROM intento
                WHERE estado='EN_PROGRESO' AND fecha_limite <= ?
                ORDER BY fecha_limite, id_intento
                """).param(Timestamp.from(ahora)).query(this::mapear).list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Intento> buscarTerminadosPorEstudianteYCaso(UUID idEstudiante, UUID idCaso) {
        return jdbc.sql("""
                SELECT i.* FROM intento i JOIN sesion s ON s.id_sesion = i.id_sesion
                WHERE i.id_estudiante = ? AND s.id_caso = ?
                    AND i.estado IN ('FINALIZADO', 'VENCIDO')
                ORDER BY i.fecha_inicio, i.id_intento
                """).params(idEstudiante.toString(), idCaso.toString()).query(this::mapear).list();
    }

    private Optional<Intento> buscar(String consulta, String id) {
        return jdbc.sql(consulta).param(id).query(this::mapear).optional();
    }

    private Intento mapear(ResultSet fila, int indice) throws SQLException {
        UUID id = UUID.fromString(fila.getString("id_intento"));
        List<UUID> preguntas = jdbc.sql("""
                SELECT id_pregunta FROM intento_pregunta
                WHERE id_intento=? ORDER BY orden
                """).param(id.toString()).query((filaPregunta, numero) ->
                UUID.fromString(filaPregunta.getString("id_pregunta"))).list();
        MapasRespuestas respuestas = cargarRespuestas(id);
        return reconstruir(fila, preguntas, respuestas.respuestas());
    }

    private Intento reconstruir(ResultSet fila, List<UUID> preguntas, LinkedHashMap<UUID, UUID> respuestas)
            throws SQLException {
        return Intento.desdePersistencia(UUID.fromString(fila.getString("id_intento")),
                UUID.fromString(fila.getString("id_sesion")), UUID.fromString(fila.getString("id_estudiante")),
                fila.getTimestamp("fecha_inicio").toInstant(), fila.getTimestamp("fecha_limite").toInstant(),
                ModoNavegacion.valueOf(fila.getString("modo_navegacion")), preguntas, respuestas,
                fila.getInt("posicion_actual"), EstadoIntento.valueOf(fila.getString("estado")));
    }

    private MapasRespuestas cargarRespuestas(UUID idIntento) {
        LinkedHashMap<UUID, UUID> respuestas = new LinkedHashMap<>();
        jdbc.sql("SELECT id_pregunta,id_opcion FROM respuesta WHERE id_intento=? ORDER BY id_pregunta")
                .param(idIntento.toString()).query((fila, numero) -> {
                    respuestas.put(UUID.fromString(fila.getString("id_pregunta")),
                            UUID.fromString(fila.getString("id_opcion")));
                    return null;
                }).list();
        return new MapasRespuestas(respuestas);
    }

    private void guardarRespuestas(Intento intento) {
        for (var respuesta : intento.respuestas().entrySet()) {
            int actualizadas = jdbc.sql("""
                    UPDATE respuesta SET id_opcion=?, fecha_actualizacion=CURRENT_TIMESTAMP
                    WHERE id_intento=? AND id_pregunta=?
                    """).params(respuesta.getValue().toString(), intento.id().toString(),
                    respuesta.getKey().toString()).update();
            if (actualizadas == 0) {
                jdbc.sql("""
                        INSERT INTO respuesta (id_intento,id_pregunta,id_opcion,fecha_actualizacion)
                        VALUES (?,?,?,CURRENT_TIMESTAMP)
                        """).params(intento.id().toString(), respuesta.getKey().toString(),
                        respuesta.getValue().toString()).update();
            }
        }
    }

    private record MapasRespuestas(LinkedHashMap<UUID, UUID> respuestas) { }
}
