package com.psicoapp.notifications.infrastructure;

import com.psicoapp.notifications.application.RepositorioNotificaciones;
import com.psicoapp.notifications.domain.EstadoNotificacion;
import com.psicoapp.notifications.domain.Notificacion;
import com.psicoapp.notifications.domain.TipoNotificacion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioNotificacionesJdbc implements RepositorioNotificaciones {
    private final JdbcClient jdbc;

    public RepositorioNotificacionesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(Notificacion n) {
        jdbc.sql("""
                INSERT INTO notificacion
                (id_notificacion,id_destinatario,correo_destinatario,tipo,asunto,mensaje,url,codigo_acceso,
                 fecha_creacion,estado)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """).params(n.id().toString(), n.idDestinatario().toString(), n.correoDestinatario(),
                n.tipo().name(), n.asunto(), n.mensaje(), n.url(), n.codigoAcceso(), n.fechaCreacion(),
                n.estado().name()).update();
    }

    @Override
    public void actualizarEstado(UUID id, String estado) {
        jdbc.sql("UPDATE notificacion SET estado=? WHERE id_notificacion=?")
                .params(estado, id.toString()).update();
    }

    @Override
    public Optional<Notificacion> buscarPorId(UUID id) {
        return jdbc.sql("SELECT * FROM notificacion WHERE id_notificacion=?")
                .param(id.toString()).query(this::mapear).optional();
    }

    @Override
    public List<Notificacion> listarPorDestinatario(UUID idDestinatario) {
        return jdbc.sql("SELECT * FROM notificacion WHERE id_destinatario=? ORDER BY fecha_creacion DESC")
                .param(idDestinatario.toString()).query(this::mapear).list();
    }

    private Notificacion mapear(ResultSet fila, int indice) throws SQLException {
        return new Notificacion(UUID.fromString(fila.getString("id_notificacion")),
                UUID.fromString(fila.getString("id_destinatario")), fila.getString("correo_destinatario"),
                TipoNotificacion.valueOf(fila.getString("tipo")), fila.getString("asunto"),
                fila.getString("mensaje"), fila.getString("url"), fila.getString("codigo_acceso"),
                fila.getTimestamp("fecha_creacion").toInstant(),
                EstadoNotificacion.valueOf(fila.getString("estado")));
    }
}
