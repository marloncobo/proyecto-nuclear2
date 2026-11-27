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
    public List<UUID> preguntasDeSesion(UUID idSesion) {
        return jdbc.sql("""
                SELECT p.id_pregunta FROM sesion s
                JOIN escenario e ON e.id_caso = s.id_caso
                JOIN pregunta p ON p.id_escenario = e.id_escenario
                WHERE s.id_sesion = ?
                ORDER BY e.orden, p.orden
                """).param(idSesion.toString()).query(String.class).list().stream()
                .map(UUID::fromString).toList();
    }

    @Override
    public boolean opcionDePregunta(UUID idPregunta, UUID idOpcion) {
        return jdbc.sql("SELECT id_opcion FROM opcion_respuesta WHERE id_pregunta = ? AND id_opcion = ?")
                .params(idPregunta.toString(), idOpcion.toString()).query(String.class).optional().isPresent();
    }

    @Override
    public void bloquearEstudiante(UUID idEstudiante) {
        jdbc.sql("SELECT id_usuario FROM usuario WHERE id_usuario = ? FOR UPDATE")
                .param(idEstudiante.toString()).query(String.class).optional()
                .orElseThrow(() -> new com.psicoapp.shared.exceptions.NoEncontrado("Estudiante no encontrado"));
    }

    @Override
    public boolean existeActivo(UUID idEstudiante) {
        return jdbc.sql("SELECT id_intento FROM intento WHERE id_estudiante = ? AND estado = 'EN_PROGRESO' LIMIT 1")
                .param(idEstudiante.toString()).query(String.class).optional().isPresent();
    }

    @Override
    public boolean tienePermisoReintento(UUID idIntento) {
        return jdbc.sql("SELECT id_intento FROM autorizacion_reintento WHERE id_intento = ?")
                .param(idIntento.toString()).query(String.class).optional().isPresent();
    }

    @Override
    public void registrarPermisoReintento(UUID idIntento, UUID idDocente, Instant fecha) {
        jdbc.sql("INSERT INTO autorizacion_reintento (id_intento,id_docente,fecha_autorizacion) VALUES (?,?,?)")
                .params(idIntento.toString(), idDocente.toString(), Timestamp.from(fecha)).update();
    }

    @Override
    public boolean retirarPermisoReintento(UUID idIntento) {
        return jdbc.sql("DELETE FROM autorizacion_reintento WHERE id_intento = ?")
                .param(idIntento.toString()).update() == 1;
    }

    @Override
    public void guardar(Intento intento) {
        jdbc.sql("""
                INSERT INTO intento (id_intento,id_sesion,id_estudiante,fecha_inicio,fecha_limite,
                    posicion_actual,modo_navegacion,estado,fecha_fin)
                VALUES (?,?,?,?,?,?,?,?,?)
                """).params(java.util.Arrays.asList(intento.id().toString(), intento.idSesion().toString(),
                intento.idEstudiante().toString(), Timestamp.from(intento.inicio()), Timestamp.from(intento.limite()),
                intento.posicionActual(), intento.modoNavegacion().name(), intento.estado().name(),
                aTimestamp(intento.fin()))).update();
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
    @Transactional(readOnly = true)
    public List<UUID> listarActivosDeSesion(UUID idSesion) {
        return jdbc.sql("""
                SELECT id_intento FROM intento WHERE id_sesion = ? AND estado = 'EN_PROGRESO'
                ORDER BY id_intento
                """).param(idSesion.toString()).query(String.class).list().stream().map(UUID::fromString).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Intento> buscarUltimo(UUID idSesion, UUID idEstudiante) {
        return jdbc.sql("""
                SELECT * FROM intento WHERE id_sesion = ? AND id_estudiante = ?
                ORDER BY fecha_inicio DESC, fecha_fin DESC, id_intento DESC LIMIT 1
                """).params(idSesion.toString(), idEstudiante.toString()).query(this::mapear).optional();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Intento> listarPorEstudiante(UUID idEstudiante) {
        return jdbc.sql("""
                SELECT * FROM intento WHERE id_estudiante = ?
                ORDER BY fecha_inicio DESC, id_intento DESC
                """).param(idEstudiante.toString()).query(this::mapear).list();
    }

    @Override
    public void actualizar(Intento intento) {
        // Las respuestas se escriben mientras el intento sigue abierto en la base.
        guardarRespuestas(intento);
        if (jdbc.sql("""
                UPDATE intento SET posicion_actual=?, estado=?, fecha_limite=?, fecha_fin=?
                WHERE id_intento=?
                """).params(java.util.Arrays.asList(intento.posicionActual(), intento.estado().name(), Timestamp.from(intento.limite()),
                aTimestamp(intento.fin()), intento.id().toString())).update() != 1) {
            throw new IllegalStateException("El intento no existe");
        }
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
        Timestamp fin = fila.getTimestamp("fecha_fin");
        return Intento.desdePersistencia(UUID.fromString(fila.getString("id_intento")),
                UUID.fromString(fila.getString("id_sesion")), UUID.fromString(fila.getString("id_estudiante")),
                fila.getTimestamp("fecha_inicio").toInstant(), fila.getTimestamp("fecha_limite").toInstant(),
                fin == null ? null : fin.toInstant(), ModoNavegacion.valueOf(fila.getString("modo_navegacion")),
                preguntas, respuestas,
                fila.getInt("posicion_actual"), EstadoIntento.valueOf(fila.getString("estado")));
    }

    private Timestamp aTimestamp(Instant instante) {
        return instante == null ? null : Timestamp.from(instante);
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
            boolean opcionValida = jdbc.sql("SELECT COUNT(*) FROM opcion_respuesta WHERE id_opcion = ? AND id_pregunta = ?")
                    .params(respuesta.getValue().toString(), respuesta.getKey().toString()).query(Integer.class).single() == 1;
            if (!opcionValida) throw new IllegalArgumentException("La opción no pertenece a la pregunta");
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
