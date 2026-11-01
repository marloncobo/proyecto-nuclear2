package com.psicoapp.sessions.infrastructure;

import com.psicoapp.sessions.application.RepositorioSesiones;
import com.psicoapp.sessions.domain.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class RepositorioSesionesJdbc implements RepositorioSesiones {
    private final JdbcClient jdbc;

    public RepositorioSesionesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(Sesion sesion) {
        if (sesion.estado().codigo() != TipoEstadoSesion.SIN_INICIAR) {
            throw new IllegalArgumentException("Toda sesion nueva debe estar sin iniciar");
        }
        jdbc.sql("""
                INSERT INTO sesion (id_sesion,id_caso,id_espacio,id_docente,fecha_inicio,fecha_fin,
                    duracion_maxima_min,modo_navegacion,estado,lugar,mensaje)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """).params(java.util.Arrays.asList(sesion.id().toString(), sesion.idCaso().toString(),
                        sesion.idEspacio().toString(), sesion.idDocente().toString(), Timestamp.from(sesion.inicioPrevisto()),
                        Timestamp.from(sesion.finPrevisto()), sesion.duracionMaximaMin(), sesion.modoNavegacion().name(),
                        sesion.estado().codigo().name(), sesion.lugar(), sesion.mensaje())).update();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sesion> buscarPorId(UUID id) {
        return jdbc.sql("SELECT * FROM sesion WHERE id_sesion = ?")
                .param(id.toString()).query(this::mapear).optional();
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Optional<Sesion> bloquearPorId(UUID id) {
        return jdbc.sql("SELECT * FROM sesion WHERE id_sesion = ? FOR UPDATE")
                .param(id.toString()).query(this::mapear).optional();
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void actualizarEstado(Sesion sesion) {
        if (jdbc.sql("UPDATE sesion SET estado = ? WHERE id_sesion = ?")
                .params(sesion.estado().codigo().name(), sesion.id().toString()).update() != 1) {
            throw new IllegalStateException("La sesion bloqueada ya no existe");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> buscarVencidas(Instant finLimite) {
        return jdbc.sql("""
                SELECT id_sesion FROM sesion
                WHERE estado IN ('SIN_INICIAR','EN_CURSO') AND fecha_fin <= ? ORDER BY id_sesion
                """).param(Timestamp.from(finLimite)).query((fila, indice) -> UUID.fromString(fila.getString("id_sesion"))).list();
    }

    private Sesion mapear(ResultSet fila, int indice) throws SQLException {
        return new Sesion(UUID.fromString(fila.getString("id_sesion")), UUID.fromString(fila.getString("id_caso")),
                UUID.fromString(fila.getString("id_espacio")), UUID.fromString(fila.getString("id_docente")),
                fila.getTimestamp("fecha_inicio").toInstant(), fila.getTimestamp("fecha_fin").toInstant(),
                fila.getInt("duracion_maxima_min"), ModoNavegacion.valueOf(fila.getString("modo_navegacion")),
                fila.getString("lugar"), fila.getString("mensaje"),
                EstadoSesion.desde(TipoEstadoSesion.valueOf(fila.getString("estado"))));
    }
}
