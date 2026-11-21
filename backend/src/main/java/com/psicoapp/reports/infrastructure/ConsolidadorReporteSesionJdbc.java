package com.psicoapp.reports.infrastructure;

import com.psicoapp.sessions.application.ConsolidadorReporteSesion;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Registra ausencias sin crear intentos ficticios para los estudiantes que no participaron. */
@Service
public class ConsolidadorReporteSesionJdbc implements ConsolidadorReporteSesion {
    private final JdbcClient jdbc;

    public ConsolidadorReporteSesionJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void consolidar(UUID idSesion, Instant fechaCierre) {
        jdbc.sql("""
                INSERT INTO ausencia_reporte (id_sesion,id_estudiante,fecha_consolidacion)
                SELECT c.id_sesion,c.id_estudiante,?
                FROM convocatoria c
                WHERE c.id_sesion=? AND c.estado='AUTORIZADA'
                  AND NOT EXISTS (SELECT 1 FROM intento i
                                  WHERE i.id_sesion=c.id_sesion AND i.id_estudiante=c.id_estudiante)
                  AND NOT EXISTS (SELECT 1 FROM ausencia_reporte a
                                  WHERE a.id_sesion=c.id_sesion AND a.id_estudiante=c.id_estudiante)
                """).params(Timestamp.from(fechaCierre), idSesion.toString()).update();
        jdbc.sql("""
                INSERT INTO detalle_ausencia_reporte
                    (id_sesion,id_estudiante,id_pregunta,puntos,valoracion)
                SELECT a.id_sesion,a.id_estudiante,p.id_pregunta,0,
                       'Ausencia: pregunta no respondida'
                FROM ausencia_reporte a
                JOIN sesion s ON s.id_sesion=a.id_sesion
                JOIN escenario e ON e.id_caso=s.id_caso
                JOIN pregunta p ON p.id_escenario=e.id_escenario
                WHERE a.id_sesion=?
                  AND NOT EXISTS (SELECT 1 FROM detalle_ausencia_reporte d
                                  WHERE d.id_sesion=a.id_sesion
                                    AND d.id_estudiante=a.id_estudiante
                                    AND d.id_pregunta=p.id_pregunta)
                """).param(idSesion.toString()).update();
    }
}
