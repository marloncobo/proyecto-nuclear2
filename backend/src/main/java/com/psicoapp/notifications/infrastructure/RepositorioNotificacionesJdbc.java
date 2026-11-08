package com.psicoapp.notifications.infrastructure;

import com.psicoapp.notifications.application.*;
import com.psicoapp.notifications.domain.SolicitudNotificacion;
import java.util.*;
import java.time.Instant;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

@Repository
public class RepositorioNotificacionesJdbc implements RepositorioEntregas, RepositorioAvisos {
    private final JdbcClient jdbc;
    private final CifradorNotificaciones cifrador;
    public RepositorioNotificacionesJdbc(JdbcClient jdbc, CifradorNotificaciones cifrador) {
        this.jdbc = jdbc; this.cifrador = cifrador;
    }
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(SolicitudNotificacion solicitud) {
        UUID id = UUID.nameUUIDFromBytes((solicitud.idEvento() + ":" + solicitud.idUsuario() + ":" + solicitud.canal())
                .getBytes(StandardCharsets.UTF_8));
        String contenido = solicitud.canal() == SolicitudNotificacion.Canal.CORREO
                ? cifrador.cifrar(solicitud.mensaje()) : solicitud.mensaje();
        jdbc.sql("""
                INSERT INTO notificacion (id_notificacion,id_evento,id_usuario,id_sesion,id_intento,
                    canal,tipo,asunto,mensaje,estado_envio,fecha_creacion,proximo_intento,intentos,leida)
                VALUES (?,?,?,?,?,?,?,?,?,'PENDIENTE',?,?,0,false)
                ON DUPLICATE KEY UPDATE id_notificacion=id_notificacion
                """).params(id.toString(), solicitud.idEvento().toString(), solicitud.idUsuario().toString(),
                    texto(solicitud.idSesion()), texto(solicitud.idIntento()), solicitud.canal().name(), solicitud.tipo(),
                    solicitud.asunto(), contenido, Timestamp.from(solicitud.fecha()), Timestamp.from(solicitud.fecha())).update();
    }
    @Override public List<UUID> pendientes(Instant ahora, int limite) {
        return jdbc.sql("""
                SELECT id_notificacion FROM notificacion WHERE estado_envio='PENDIENTE' AND proximo_intento<=?
                ORDER BY proximo_intento,id_notificacion LIMIT ?
                """).params(Timestamp.from(ahora), limite).query((fila, indice) -> UUID.fromString(fila.getString(1))).list();
    }
    @Override public Optional<Entrega> bloquearPendiente(UUID id, Instant ahora) {
        return jdbc.sql("""
                SELECT * FROM notificacion WHERE id_notificacion=? AND estado_envio='PENDIENTE'
                AND proximo_intento<=? FOR UPDATE
                """).params(id.toString(), Timestamp.from(ahora)).query((fila, indice) -> new Entrega(id,
                    UUID.fromString(fila.getString("id_usuario")), identificador(fila.getString("id_sesion")),
                    fila.getString("canal"), fila.getString("tipo"), fila.getString("asunto"),
                    fila.getString("mensaje"), fila.getInt("intentos"))).optional();
    }
    @Override public String recuperarContenido(Entrega entrega) { return cifrador.descifrar(entrega.contenido()); }
    @Override public void confirmar(UUID id, Instant ahora) {
        jdbc.sql("""
                UPDATE notificacion SET estado_envio='ENVIADA',fecha_envio=?,ultimo_error=NULL,
                mensaje=CASE WHEN canal='CORREO' THEN '' ELSE mensaje END WHERE id_notificacion=?
                """).params(Timestamp.from(ahora), id.toString()).update();
    }
    @Override public void posponer(UUID id, int intentos, Instant siguiente) {
        jdbc.sql("UPDATE notificacion SET intentos=?,proximo_intento=?,ultimo_error='ENVIO_FALLIDO' WHERE id_notificacion=?")
                .params(intentos, Timestamp.from(siguiente), id.toString()).update();
    }
    @Override public void cancelarPendientes(UUID sesion, UUID estudiante, String tipo) {
        jdbc.sql("""
                UPDATE notificacion SET estado_envio='CANCELADA',mensaje='' WHERE id_sesion=? AND id_usuario=?
                AND canal='CORREO' AND tipo=? AND estado_envio='PENDIENTE'
                """).params(sesion.toString(), estudiante.toString(), tipo).update();
    }
    @Override public List<Aviso> listar(UUID usuario, int limite, int desplazamiento) {
        return jdbc.sql("""
                SELECT * FROM notificacion WHERE id_usuario=? AND canal='INTERNO' AND estado_envio='ENVIADA'
                ORDER BY fecha_creacion DESC,id_notificacion DESC LIMIT ? OFFSET ?
                """).params(usuario.toString(), limite, desplazamiento).query((fila, indice) -> new Aviso(
                    UUID.fromString(fila.getString("id_notificacion")), identificador(fila.getString("id_sesion")),
                    identificador(fila.getString("id_intento")), fila.getString("tipo"), fila.getString("mensaje"),
                    fila.getTimestamp("fecha_creacion").toInstant(), fila.getBoolean("leida"))).list();
    }
    @Override public boolean marcarLeida(UUID usuario, UUID aviso) {
        // Una lectura repetida sigue siendo exitosa, tambien con MySQL configurado para contar cambios.
        var ids = jdbc.sql("""
                SELECT id_notificacion FROM notificacion WHERE id_notificacion=? AND id_usuario=?
                AND canal='INTERNO' AND estado_envio='ENVIADA' FOR UPDATE
                """).params(aviso.toString(), usuario.toString()).query(String.class).list();
        if (ids.isEmpty()) return false;
        jdbc.sql("UPDATE notificacion SET leida=true WHERE id_notificacion=?").param(aviso.toString()).update();
        return true;
    }
    private String texto(UUID id) { return id == null ? null : id.toString(); }
    private UUID identificador(String texto) { return texto == null ? null : UUID.fromString(texto); }
}
