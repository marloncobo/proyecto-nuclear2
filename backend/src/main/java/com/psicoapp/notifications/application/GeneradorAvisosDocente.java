package com.psicoapp.notifications.application;

import com.psicoapp.notifications.domain.SolicitudNotificacion;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.psicoapp.notifications.domain.SolicitudNotificacion.Canal.INTERNO;

/** Reconstruye avisos docentes desde el estado durable para tolerar reinicios. */
@Service
public class GeneradorAvisosDocente {
    private record ResumenSesion(UUID id, UUID docente, String caso, Instant inicio,
            String estado, int convocados, int pendientes, int intentos, int evaluacionesPendientes) { }

    private final JdbcClient jdbc;
    private final RepositorioEntregas entregas;

    public GeneradorAvisosDocente(JdbcClient jdbc, RepositorioEntregas entregas) {
        this.jdbc = jdbc;
        this.entregas = entregas;
    }

    @Transactional
    public void generar(Instant ahora, Duration anticipacion) {
        if (ahora == null || anticipacion == null || anticipacion.isNegative() || anticipacion.isZero()) {
            throw new IllegalArgumentException("El instante y la anticipacion positiva son obligatorios");
        }
        for (ResumenSesion sesion : sesionesRelevantes(ahora, anticipacion)) {
            if ("SIN_INICIAR".equals(sesion.estado()) && sesion.inicio().isAfter(ahora)) {
                registrar(sesion, "RECORDATORIO_SESION", "Recordatorio de sesión",
                        "La sesión de " + sesion.caso() + " comenzará el " + sesion.inicio() + ".", ahora);
                continue;
            }
            if (!"FINALIZADO".equals(sesion.estado())) continue;
            registrar(sesion, "FINALIZACION_SESION", "Sesión finalizada",
                    "La sesión de " + sesion.caso()
                            + " ha finalizado. Ya puedes revisar la participación.", ahora);
            if (sesion.pendientes() > 0) {
                registrar(sesion, "ESTUDIANTES_PENDIENTES", "Estudiantes pendientes",
                        sesion.pendientes() + " de " + sesion.convocados()
                                + " estudiantes no completaron la sesión.", ahora);
            }
            if (sesion.intentos() > 0 && sesion.evaluacionesPendientes() == 0) {
                registrar(sesion, "EVALUACIONES_DISPONIBLES", "Evaluaciones disponibles",
                        "Los resultados de la sesión " + sesion.caso() + " ya están disponibles.", ahora);
            }
            registrar(sesion, "REPORTE_DISPONIBLE", "Reporte disponible",
                    "El reporte de desempeño de " + sesion.caso() + " está disponible.", ahora);
        }
    }

    private List<ResumenSesion> sesionesRelevantes(Instant ahora, Duration anticipacion) {
        return jdbc.sql("""
                SELECT s.id_sesion,s.id_docente,s.fecha_inicio,s.estado,ca.nombre,
                  (SELECT COUNT(DISTINCT c.id_estudiante) FROM convocatoria c
                   WHERE c.id_sesion=s.id_sesion AND c.estado='AUTORIZADA') AS convocados,
                  (SELECT COUNT(DISTINCT c.id_estudiante) FROM convocatoria c
                   WHERE c.id_sesion=s.id_sesion AND c.estado='AUTORIZADA'
                     AND NOT EXISTS (SELECT 1 FROM intento i WHERE i.id_sesion=s.id_sesion
                       AND i.id_estudiante=c.id_estudiante AND i.estado IN ('FINALIZADO','VENCIDO'))) AS pendientes,
                  (SELECT COUNT(*) FROM intento i WHERE i.id_sesion=s.id_sesion
                     AND i.estado IN ('FINALIZADO','VENCIDO')) AS intentos,
                  (SELECT COUNT(*) FROM intento i LEFT JOIN evaluacion e ON e.id_intento=i.id_intento
                   WHERE i.id_sesion=s.id_sesion AND i.estado IN ('FINALIZADO','VENCIDO')
                     AND e.id_intento IS NULL) AS evaluaciones_pendientes
                FROM sesion s JOIN caso ca ON ca.id_caso=s.id_caso
                WHERE (s.estado='SIN_INICIAR' AND s.fecha_inicio>? AND s.fecha_inicio<=?)
                   OR s.estado='FINALIZADO'
                """).params(Timestamp.from(ahora), Timestamp.from(ahora.plus(anticipacion)))
                .query((fila, indice) -> new ResumenSesion(
                        UUID.fromString(fila.getString("id_sesion")),
                        UUID.fromString(fila.getString("id_docente")), fila.getString("nombre"),
                        fila.getTimestamp("fecha_inicio").toInstant(), fila.getString("estado"),
                        fila.getInt("convocados"), fila.getInt("pendientes"), fila.getInt("intentos"),
                        fila.getInt("evaluaciones_pendientes"))).list();
    }

    private void registrar(ResumenSesion sesion, String tipo, String asunto, String mensaje, Instant fecha) {
        UUID evento = UUID.nameUUIDFromBytes(("aviso-docente:" + tipo + ":" + sesion.id())
                .getBytes(StandardCharsets.UTF_8));
        entregas.registrar(new SolicitudNotificacion(evento, sesion.docente(), sesion.id(), null,
                INTERNO, tipo, asunto, mensaje, fecha));
    }
}
