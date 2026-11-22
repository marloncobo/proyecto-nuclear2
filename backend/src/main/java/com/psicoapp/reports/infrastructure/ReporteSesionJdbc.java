package com.psicoapp.reports.infrastructure;

import com.psicoapp.sessions.application.ConsultaSesionesDocente;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteSesionJdbc {
    public record DetalleAusencia(UUID idPregunta, BigDecimal puntos, String valoracion) { }
    public record Ausencia(Instant fechaConsolidacion, List<DetalleAusencia> detalles) {
        public Ausencia { detalles = List.copyOf(detalles); }
    }
    public record Fila(UUID idEstudiante, String nombreEstudiante, String correoEstudiante,
            UUID idIntento, String estado, BigDecimal calificacion, Integer preguntasRespondidas,
            Integer preguntasTotales, Ausencia ausencia) { }

    private final JdbcClient jdbc;
    private final ConsultaSesionesDocente sesiones;

    public ReporteSesionJdbc(JdbcClient jdbc, ConsultaSesionesDocente sesiones) {
        this.jdbc = jdbc;
        this.sesiones = sesiones;
    }

    @Transactional(readOnly = true)
    public List<Fila> consultar(UUID docente, UUID sesion) {
        sesiones.exigirResponsable(docente, sesion);
        List<Fila> filas = jdbc.sql("""
                SELECT c.id_estudiante, u.nombre, u.correo, i.id_intento,
                       CASE WHEN i.id_intento IS NOT NULL THEN i.estado
                            WHEN a.id_estudiante IS NOT NULL THEN 'AUSENTE'
                            WHEN s.estado = 'FINALIZADO' THEN 'AUSENTE'
                            ELSE 'SIN_INTENTO' END AS estado,
                       CASE WHEN i.id_intento IS NULL AND s.estado = 'FINALIZADO'
                            THEN 0 ELSE ev.calificacion END AS calificacion,
                       CASE WHEN i.id_intento IS NOT NULL THEN
                           (SELECT COUNT(*) FROM respuesta r WHERE r.id_intento = i.id_intento)
                       END AS respondidas,
                       CASE WHEN i.id_intento IS NOT NULL THEN
                           (SELECT COUNT(*) FROM intento_pregunta ip WHERE ip.id_intento = i.id_intento)
                       END AS totales,
                       a.fecha_consolidacion
                FROM convocatoria c
                JOIN sesion s ON s.id_sesion = c.id_sesion
                JOIN usuario u ON u.id_usuario = c.id_estudiante
                LEFT JOIN intento i ON i.id_sesion = c.id_sesion AND i.id_estudiante = c.id_estudiante
                LEFT JOIN evaluacion ev ON ev.id_intento = i.id_intento
                LEFT JOIN ausencia_reporte a ON a.id_sesion = c.id_sesion AND a.id_estudiante = c.id_estudiante
                WHERE c.id_sesion = ? AND (c.estado = 'AUTORIZADA' OR i.id_intento IS NOT NULL)
                ORDER BY u.nombre, u.correo, i.fecha_inicio, i.id_intento
                """).param(sesion.toString()).query((fila, indice) -> {
                    String intento = fila.getString("id_intento");
                    String fechaConsolidacion = fila.getString("fecha_consolidacion");
                    UUID estudiante = UUID.fromString(fila.getString("id_estudiante"));
                    Ausencia ausencia = fechaConsolidacion == null ? null : new Ausencia(
                            fila.getTimestamp("fecha_consolidacion").toInstant(), List.of());
                    return new Fila(estudiante, fila.getString("nombre"), fila.getString("correo"),
                            intento == null ? null : UUID.fromString(intento),
                            fila.getString("estado"), fila.getBigDecimal("calificacion"),
                            fila.getObject("respondidas", Integer.class), fila.getObject("totales", Integer.class), ausencia);
                }).list();
        return filas.stream().map(fila -> fila.ausencia() == null ? fila : new Fila(
                fila.idEstudiante(), fila.nombreEstudiante(), fila.correoEstudiante(), fila.idIntento(),
                fila.estado(), fila.calificacion(), fila.preguntasRespondidas(), fila.preguntasTotales(), new Ausencia(fila.ausencia().fechaConsolidacion(),
                        consultarDetallesAusencia(sesion, fila.idEstudiante())))).toList();
    }

    private List<DetalleAusencia> consultarDetallesAusencia(UUID sesion, UUID estudiante) {
        return jdbc.sql("""
                SELECT d.id_pregunta,d.puntos,d.valoracion
                FROM detalle_ausencia_reporte d
                JOIN pregunta p ON p.id_pregunta=d.id_pregunta
                JOIN escenario e ON e.id_escenario=p.id_escenario
                WHERE d.id_sesion=? AND d.id_estudiante=?
                ORDER BY e.orden,p.orden,p.id_pregunta
                """).params(sesion.toString(), estudiante.toString()).query((fila, indice) ->
                new DetalleAusencia(UUID.fromString(fila.getString("id_pregunta")),
                        fila.getBigDecimal("puntos"), fila.getString("valoracion"))).list();
    }
}
