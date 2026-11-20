package com.psicoapp.reports.infrastructure;

import com.psicoapp.sessions.application.ConsultaSesionesDocente;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteSesionJdbc {
    public record Fila(UUID idEstudiante, UUID idIntento, String estado, BigDecimal calificacion) { }

    private final JdbcClient jdbc;
    private final ConsultaSesionesDocente sesiones;

    public ReporteSesionJdbc(JdbcClient jdbc, ConsultaSesionesDocente sesiones) {
        this.jdbc = jdbc;
        this.sesiones = sesiones;
    }

    @Transactional(readOnly = true)
    public List<Fila> consultar(UUID docente, UUID sesion) {
        sesiones.exigirResponsable(docente, sesion);
        return jdbc.sql("""
                SELECT c.id_estudiante, i.id_intento,
                       CASE WHEN i.id_intento IS NOT NULL THEN i.estado
                            WHEN s.estado = 'FINALIZADO' THEN 'AUSENTE'
                            ELSE 'SIN_INTENTO' END AS estado,
                       CASE WHEN i.id_intento IS NULL AND s.estado = 'FINALIZADO'
                            THEN 0 ELSE e.calificacion END AS calificacion
                FROM convocatoria c
                JOIN sesion s ON s.id_sesion = c.id_sesion
                LEFT JOIN intento i ON i.id_sesion = c.id_sesion AND i.id_estudiante = c.id_estudiante
                LEFT JOIN evaluacion e ON e.id_intento = i.id_intento
                WHERE c.id_sesion = ? AND (c.estado = 'AUTORIZADA' OR i.id_intento IS NOT NULL)
                ORDER BY c.id_estudiante, i.fecha_inicio, i.id_intento
                """).param(sesion.toString()).query((fila, indice) -> {
                    String intento = fila.getString("id_intento");
                    return new Fila(UUID.fromString(fila.getString("id_estudiante")),
                            intento == null ? null : UUID.fromString(intento),
                            fila.getString("estado"), fila.getBigDecimal("calificacion"));
                }).list();
    }
}
