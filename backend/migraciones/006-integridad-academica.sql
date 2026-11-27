-- Ejecutar sobre el esquema de Psicoapp después de schema.sql y las migraciones previas.
DELIMITER $$

-- La unicidad se aplica también cuando dos transacciones intentan iniciar a la vez.
CREATE UNIQUE INDEX uq_intento_activo_estudiante
    ON intento ((CASE WHEN estado = 'EN_PROGRESO' THEN id_estudiante ELSE NULL END))$$

CREATE SQL SECURITY INVOKER VIEW v_integridad_casos AS
SELECT c.id_caso, c.id_caso AS id_elemento, 'PUBLICACION_INCOMPLETA' AS incidencia
FROM caso c
WHERE c.estado = 'PUBLICADO'
  AND (c.tiempo_estimado_min IS NULL
       OR NOT EXISTS (SELECT 1 FROM escenario e WHERE e.id_caso = c.id_caso)
       OR NOT EXISTS (SELECT 1 FROM rubrica r WHERE r.id_caso = c.id_caso))
UNION ALL
SELECT e.id_caso, e.id_escenario, 'ESCENARIO_SIN_PREGUNTAS'
FROM escenario e JOIN caso c ON c.id_caso = e.id_caso
WHERE c.estado = 'PUBLICADO'
  AND NOT EXISTS (SELECT 1 FROM pregunta p WHERE p.id_escenario = e.id_escenario)
UNION ALL
SELECT e.id_caso, p.id_pregunta, 'PREGUNTA_SIN_OPCIONES_DE_AMBOS_TIPOS'
FROM pregunta p
JOIN escenario e ON e.id_escenario = p.id_escenario
JOIN caso c ON c.id_caso = e.id_caso
WHERE c.estado = 'PUBLICADO'
  AND (NOT EXISTS (SELECT 1 FROM opcion_respuesta o WHERE o.id_pregunta = p.id_pregunta AND o.es_correcta = 1)
       OR NOT EXISTS (SELECT 1 FROM opcion_respuesta o WHERE o.id_pregunta = p.id_pregunta AND o.es_correcta = 0))
UNION ALL
SELECT r.id_caso, r.id_rubrica, 'PONDERACIONES_INVALIDAS'
FROM rubrica r JOIN caso c ON c.id_caso = r.id_caso
LEFT JOIN criterio_rubrica cr ON cr.id_rubrica = r.id_rubrica
WHERE c.estado = 'PUBLICADO'
GROUP BY r.id_caso, r.id_rubrica
HAVING COALESCE(SUM(cr.ponderacion), 0) <> 1
UNION ALL
SELECT e.id_caso, p.id_pregunta, 'PREGUNTA_SIN_CRITERIO'
FROM pregunta p
JOIN escenario e ON e.id_escenario = p.id_escenario
JOIN caso c ON c.id_caso = e.id_caso
WHERE c.estado = 'PUBLICADO' AND NOT EXISTS (
    SELECT 1 FROM criterio_pregunta cp
    JOIN criterio_rubrica cr ON cr.id_criterio = cp.id_criterio
    JOIN rubrica r ON r.id_rubrica = cr.id_rubrica
    WHERE cp.id_pregunta = p.id_pregunta AND r.id_caso = e.id_caso
)
UNION ALL
SELECT r.id_caso, cp.id_pregunta, 'CRITERIO_DE_OTRO_CASO'
FROM criterio_pregunta cp
JOIN criterio_rubrica cr ON cr.id_criterio = cp.id_criterio
JOIN rubrica r ON r.id_rubrica = cr.id_rubrica
JOIN pregunta p ON p.id_pregunta = cp.id_pregunta
JOIN escenario e ON e.id_escenario = p.id_escenario
WHERE r.id_caso <> e.id_caso$$

CREATE SQL SECURITY INVOKER VIEW v_integridad_participacion AS
SELECT s.id_sesion, NULL AS id_intento, NULL AS id_pregunta, 'CONFIGURACION_SESION_INVALIDA' AS incidencia
FROM sesion s JOIN caso c ON c.id_caso = s.id_caso
WHERE c.estado = 'BORRADOR' OR c.tiempo_estimado_min IS NULL
   OR s.id_docente <> c.id_usuario_docente OR s.duracion_maxima_min > c.tiempo_estimado_min
   OR TIMESTAMPDIFF(MINUTE, s.fecha_inicio, s.fecha_fin) < s.duracion_maxima_min
UNION ALL
SELECT i.id_sesion, i.id_intento, NULL, 'INTENTO_SIN_CONVOCATORIA'
FROM intento i LEFT JOIN convocatoria c ON c.id_sesion = i.id_sesion AND c.id_estudiante = i.id_estudiante
WHERE c.id_convocatoria IS NULL
UNION ALL
SELECT i.id_sesion, i.id_intento, NULL, 'INTENTO_ACTIVO_EN_SESION_O_CONVOCATORIA_CERRADA'
FROM intento i
JOIN sesion s ON s.id_sesion = i.id_sesion
JOIN convocatoria c ON c.id_sesion = i.id_sesion AND c.id_estudiante = i.id_estudiante
WHERE i.estado = 'EN_PROGRESO' AND (s.estado <> 'EN_CURSO' OR c.estado = 'RETIRADA')
UNION ALL
SELECT i.id_sesion, i.id_intento, NULL, 'TIEMPOS_DE_INTENTO_INVALIDOS'
FROM intento i JOIN sesion s ON s.id_sesion = i.id_sesion
WHERE i.fecha_limite > LEAST(s.fecha_fin, DATE_ADD(i.fecha_inicio, INTERVAL s.duracion_maxima_min MINUTE))
   OR (i.fecha_fin IS NOT NULL AND (i.estado = 'EN_PROGRESO' OR i.fecha_fin < i.fecha_inicio))
UNION ALL
SELECT i.id_sesion, i.id_intento, ip.id_pregunta, 'PREGUNTA_DE_OTRO_CASO'
FROM intento_pregunta ip
JOIN intento i ON i.id_intento = ip.id_intento
JOIN sesion s ON s.id_sesion = i.id_sesion
JOIN pregunta p ON p.id_pregunta = ip.id_pregunta
JOIN escenario e ON e.id_escenario = p.id_escenario
WHERE s.id_caso <> e.id_caso
UNION ALL
SELECT i.id_sesion, i.id_intento, r.id_pregunta, 'RESPUESTA_CON_OPCION_AJENA'
FROM respuesta r JOIN intento i ON i.id_intento = r.id_intento
JOIN opcion_respuesta o ON o.id_opcion = r.id_opcion
WHERE r.id_pregunta <> o.id_pregunta$$

CREATE SQL SECURITY INVOKER VIEW v_integridad_resultados AS
SELECT i.id_sesion, i.id_estudiante, i.id_intento, NULL AS id_pregunta, 'EVALUACION_DE_INTENTO_ACTIVO' AS incidencia
FROM evaluacion ev JOIN intento i ON i.id_intento = ev.id_intento
WHERE i.estado = 'EN_PROGRESO'
UNION ALL
SELECT i.id_sesion, i.id_estudiante, i.id_intento, NULL, 'NOTA_FUERA_DE_ESCALA'
FROM evaluacion ev
JOIN intento i ON i.id_intento = ev.id_intento
JOIN sesion s ON s.id_sesion = i.id_sesion
LEFT JOIN rubrica r ON r.id_caso = s.id_caso
WHERE r.id_rubrica IS NULL OR ev.calificacion > r.escala_maxima
UNION ALL
SELECT i.id_sesion, i.id_estudiante, i.id_intento, d.id_pregunta, 'DETALLE_DE_EVALUACION_INCONSISTENTE'
FROM detalle_evaluacion d
JOIN intento i ON i.id_intento = d.id_intento
LEFT JOIN intento_pregunta ip ON ip.id_intento = d.id_intento AND ip.id_pregunta = d.id_pregunta
LEFT JOIN respuesta r ON r.id_intento = d.id_intento AND r.id_pregunta = d.id_pregunta
LEFT JOIN opcion_respuesta o ON o.id_opcion = r.id_opcion AND o.id_pregunta = r.id_pregunta
WHERE ip.id_pregunta IS NULL OR NOT (d.id_opcion <=> r.id_opcion)
   OR d.contestada <> (r.id_opcion IS NOT NULL) OR d.correcta <> COALESCE(o.es_correcta, 0)
   OR (d.correcta = 0 AND d.puntos <> 0)
UNION ALL
SELECT i.id_sesion, i.id_estudiante, i.id_intento, ip.id_pregunta, 'DETALLE_DE_EVALUACION_FALTANTE'
FROM evaluacion ev
JOIN intento i ON i.id_intento = ev.id_intento
JOIN intento_pregunta ip ON ip.id_intento = ev.id_intento
LEFT JOIN detalle_evaluacion d ON d.id_intento = ip.id_intento AND d.id_pregunta = ip.id_pregunta
WHERE d.id_pregunta IS NULL
UNION ALL
SELECT a.id_sesion, a.id_estudiante, NULL, NULL, 'AUSENCIA_INCONSISTENTE'
FROM ausencia_reporte a
JOIN sesion s ON s.id_sesion = a.id_sesion
LEFT JOIN convocatoria c ON c.id_sesion = a.id_sesion AND c.id_estudiante = a.id_estudiante
WHERE s.estado <> 'FINALIZADO' OR c.id_convocatoria IS NULL OR c.estado <> 'AUTORIZADA'
   OR EXISTS (SELECT 1 FROM intento i WHERE i.id_sesion = a.id_sesion AND i.id_estudiante = a.id_estudiante)$$

CREATE TRIGGER integridad_caso_insertar
BEFORE INSERT ON caso FOR EACH ROW
BEGIN
    IF NEW.estado <> 'BORRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un caso nuevo debe comenzar como BORRADOR';
    END IF;
END$$

CREATE TRIGGER integridad_caso_actualizar
BEFORE UPDATE ON caso FOR EACH ROW
BEGIN
    IF OLD.estado <> 'BORRADOR' AND (
        NEW.id_caso <> OLD.id_caso OR NEW.id_usuario_docente <> OLD.id_usuario_docente
        OR NOT (NEW.tiempo_estimado_min <=> OLD.tiempo_estimado_min)
        OR BINARY NEW.nombre <> BINARY OLD.nombre
        OR BINARY NEW.contexto <> BINARY OLD.contexto
        OR BINARY NEW.desarrollo_situacional <> BINARY OLD.desarrollo_situacional
        OR NEW.estado = 'BORRADOR' OR (OLD.estado = 'ARCHIVADO' AND NEW.estado <> OLD.estado)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un caso publicado solo puede archivarse';
    END IF;

    IF OLD.estado = 'BORRADOR' AND NEW.estado = 'PUBLICADO' AND (
        NEW.tiempo_estimado_min IS NULL
        OR NOT EXISTS (SELECT 1 FROM escenario e WHERE e.id_caso = NEW.id_caso)
        OR NOT EXISTS (SELECT 1 FROM rubrica r WHERE r.id_caso = NEW.id_caso)
        OR EXISTS (
            SELECT 1 FROM escenario e
            WHERE e.id_caso = NEW.id_caso
              AND NOT EXISTS (SELECT 1 FROM pregunta p WHERE p.id_escenario = e.id_escenario)
        )
        OR EXISTS (
            SELECT 1 FROM pregunta p JOIN escenario e ON e.id_escenario = p.id_escenario
            WHERE e.id_caso = NEW.id_caso
              AND (NOT EXISTS (SELECT 1 FROM opcion_respuesta o WHERE o.id_pregunta = p.id_pregunta AND o.es_correcta = 1)
                   OR NOT EXISTS (SELECT 1 FROM opcion_respuesta o WHERE o.id_pregunta = p.id_pregunta AND o.es_correcta = 0))
        )
        OR EXISTS (
            SELECT 1 FROM pregunta p JOIN escenario e ON e.id_escenario = p.id_escenario
            WHERE e.id_caso = NEW.id_caso AND NOT EXISTS (
                SELECT 1 FROM criterio_pregunta cp
                JOIN criterio_rubrica cr ON cr.id_criterio = cp.id_criterio
                JOIN rubrica r ON r.id_rubrica = cr.id_rubrica
                WHERE cp.id_pregunta = p.id_pregunta AND r.id_caso = e.id_caso
            )
        )
        OR 1 <> (
            SELECT COALESCE(SUM(cr.ponderacion), 0)
            FROM rubrica r JOIN criterio_rubrica cr ON cr.id_rubrica = r.id_rubrica
            WHERE r.id_caso = NEW.id_caso
        )
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La estructura del caso o su rubrica esta incompleta';
    END IF;
END$$

CREATE TRIGGER integridad_criterio_pregunta_insertar
BEFORE INSERT ON criterio_pregunta FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM criterio_rubrica cr
        JOIN rubrica r ON r.id_rubrica = cr.id_rubrica
        JOIN escenario e ON e.id_caso = r.id_caso
        JOIN pregunta p ON p.id_escenario = e.id_escenario
        WHERE cr.id_criterio = NEW.id_criterio AND p.id_pregunta = NEW.id_pregunta
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El criterio y la pregunta deben pertenecer al mismo caso';
    END IF;
END$$

CREATE TRIGGER integridad_criterio_pregunta_actualizar
BEFORE UPDATE ON criterio_pregunta FOR EACH ROW
BEGIN
    IF NEW.id_criterio <> OLD.id_criterio OR NEW.id_pregunta <> OLD.id_pregunta THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cambie el vinculo del criterio mediante baja y alta';
    END IF;
END$$

CREATE TRIGGER integridad_sesion_insertar
BEFORE INSERT ON sesion FOR EACH ROW
BEGIN
    DECLARE estado_caso VARCHAR(20);
    DECLARE docente_caso CHAR(36);
    DECLARE tiempo_caso INT;

    SELECT estado, id_usuario_docente, tiempo_estimado_min INTO estado_caso, docente_caso, tiempo_caso
    FROM caso WHERE id_caso = NEW.id_caso FOR SHARE;

    IF NEW.estado <> 'SIN_INICIAR' OR NOT (estado_caso <=> 'PUBLICADO')
        OR NEW.id_docente <> docente_caso OR tiempo_caso IS NULL OR NEW.duracion_maxima_min > tiempo_caso
        OR TIMESTAMPDIFF(MINUTE, NEW.fecha_inicio, NEW.fecha_fin) < NEW.duracion_maxima_min THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La sesion requiere un caso publicado del docente y una duracion valida';
    END IF;
END$$

CREATE TRIGGER integridad_sesion_actualizar
BEFORE UPDATE ON sesion FOR EACH ROW
BEGIN
    IF NEW.id_sesion <> OLD.id_sesion OR NEW.id_caso <> OLD.id_caso OR NEW.id_docente <> OLD.id_docente
        OR NEW.id_espacio <> OLD.id_espacio OR NEW.fecha_inicio <> OLD.fecha_inicio OR NEW.fecha_fin <> OLD.fecha_fin
        OR NEW.duracion_maxima_min <> OLD.duracion_maxima_min OR NEW.modo_navegacion <> OLD.modo_navegacion THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La configuracion de una sesion programada es fija';
    END IF;
    IF (OLD.estado = 'FINALIZADO' AND NEW.estado <> OLD.estado)
        OR (OLD.estado = 'EN_CURSO' AND NEW.estado = 'SIN_INICIAR') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La sesion no puede volver a un estado anterior';
    END IF;
END$$

CREATE TRIGGER integridad_intento_insertar
BEFORE INSERT ON intento FOR EACH ROW
BEGIN
    DECLARE estado_sesion VARCHAR(20);
    DECLARE fin_sesion TIMESTAMP;
    DECLARE duracion_sesion INT;
    DECLARE modo_sesion VARCHAR(12);
    DECLARE estado_convocatoria VARCHAR(20);

    SELECT estado, fecha_fin, duracion_maxima_min, modo_navegacion
    INTO estado_sesion, fin_sesion, duracion_sesion, modo_sesion
    FROM sesion WHERE id_sesion = NEW.id_sesion FOR SHARE;

    IF NOT EXISTS (
        SELECT 1 FROM usuario u JOIN rol r ON r.id_rol = u.id_rol
        WHERE u.id_usuario = NEW.id_estudiante AND u.activo = 1 AND r.nombre = 'ESTUDIANTE'
        FOR SHARE
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo un estudiante activo puede iniciar un intento';
    END IF;

    SELECT estado INTO estado_convocatoria FROM convocatoria
    WHERE id_sesion = NEW.id_sesion AND id_estudiante = NEW.id_estudiante FOR SHARE;

    IF NOT (estado_sesion <=> 'EN_CURSO') OR NOT (estado_convocatoria <=> 'AUTORIZADA') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El intento requiere una convocatoria autorizada y una sesion en curso';
    END IF;
    IF NEW.estado <> 'EN_PROGRESO' OR NEW.fecha_fin IS NOT NULL OR NEW.posicion_actual <> 0
        OR NEW.modo_navegacion <> modo_sesion
        OR NEW.fecha_limite > LEAST(fin_sesion, DATE_ADD(NEW.fecha_inicio, INTERVAL duracion_sesion MINUTE)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El estado inicial o el limite del intento es invalido';
    END IF;
END$$

CREATE TRIGGER integridad_intento_actualizar
BEFORE UPDATE ON intento FOR EACH ROW
BEGIN
    IF NEW.id_intento <> OLD.id_intento OR NEW.id_sesion <> OLD.id_sesion
        OR NEW.id_estudiante <> OLD.id_estudiante OR NEW.fecha_inicio <> OLD.fecha_inicio
        OR NEW.fecha_limite <> OLD.fecha_limite OR NEW.modo_navegacion <> OLD.modo_navegacion THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Los datos iniciales del intento son fijos';
    END IF;
    IF (OLD.estado <> 'EN_PROGRESO' AND (
            NEW.estado <> OLD.estado OR NEW.posicion_actual <> OLD.posicion_actual
            OR NOT (NEW.fecha_fin <=> OLD.fecha_fin)
        ))
        OR (NEW.estado = 'EN_PROGRESO' AND NEW.fecha_fin IS NOT NULL)
        OR (OLD.estado = 'EN_PROGRESO' AND NEW.estado <> OLD.estado AND (
            NEW.fecha_fin IS NULL OR NEW.fecha_fin < NEW.fecha_inicio
        )) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La transicion o la fecha de cierre del intento es invalida';
    END IF;
END$$

CREATE TRIGGER integridad_intento_pregunta_insertar
BEFORE INSERT ON intento_pregunta FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM intento i
        JOIN sesion s ON s.id_sesion = i.id_sesion
        JOIN escenario e ON e.id_caso = s.id_caso
        JOIN pregunta p ON p.id_escenario = e.id_escenario
        WHERE i.id_intento = NEW.id_intento AND i.estado = 'EN_PROGRESO' AND p.id_pregunta = NEW.id_pregunta
        FOR SHARE
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La pregunta debe pertenecer al caso de un intento activo';
    END IF;
END$$

CREATE TRIGGER integridad_intento_pregunta_actualizar
BEFORE UPDATE ON intento_pregunta FOR EACH ROW
BEGIN
    IF NEW.id_intento <> OLD.id_intento OR NEW.id_pregunta <> OLD.id_pregunta OR NEW.orden <> OLD.orden THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El orden y las preguntas del intento son fijos';
    END IF;
END$$

CREATE TRIGGER integridad_respuesta_insertar
BEFORE INSERT ON respuesta FOR EACH ROW
BEGIN
    IF NOT EXISTS (SELECT 1 FROM intento WHERE id_intento = NEW.id_intento AND estado = 'EN_PROGRESO' FOR SHARE)
        OR NOT EXISTS (SELECT 1 FROM opcion_respuesta WHERE id_opcion = NEW.id_opcion AND id_pregunta = NEW.id_pregunta) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La respuesta requiere un intento activo y una opcion de su pregunta';
    END IF;
END$$

CREATE TRIGGER integridad_respuesta_actualizar
BEFORE UPDATE ON respuesta FOR EACH ROW
BEGIN
    IF NEW.id_intento <> OLD.id_intento OR NEW.id_pregunta <> OLD.id_pregunta
        OR NOT EXISTS (SELECT 1 FROM intento WHERE id_intento = OLD.id_intento AND estado = 'EN_PROGRESO' FOR SHARE)
        OR NOT EXISTS (SELECT 1 FROM opcion_respuesta WHERE id_opcion = NEW.id_opcion AND id_pregunta = NEW.id_pregunta) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La respuesta requiere un intento activo y una opcion de su pregunta';
    END IF;
END$$

CREATE TRIGGER integridad_respuesta_borrar
BEFORE DELETE ON respuesta FOR EACH ROW
BEGIN
    IF NOT EXISTS (SELECT 1 FROM intento WHERE id_intento = OLD.id_intento AND estado = 'EN_PROGRESO' FOR SHARE) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se pueden borrar respuestas de un intento cerrado';
    END IF;
END$$

CREATE TRIGGER integridad_opcion_respuesta_actualizar
BEFORE UPDATE ON opcion_respuesta FOR EACH ROW
BEGIN
    IF NEW.id_pregunta <> OLD.id_pregunta THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una opcion debe conservar su pregunta original';
    END IF;
END$$

CREATE TRIGGER integridad_evaluacion_insertar
BEFORE INSERT ON evaluacion FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM intento i JOIN sesion s ON s.id_sesion = i.id_sesion
        JOIN rubrica r ON r.id_caso = s.id_caso
        WHERE i.id_intento = NEW.id_intento AND i.estado IN ('FINALIZADO', 'VENCIDO')
          AND NEW.calificacion <= r.escala_maxima
        FOR SHARE
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La evaluacion requiere un intento cerrado y una nota dentro de la escala';
    END IF;
END$$

CREATE TRIGGER integridad_evaluacion_actualizar
BEFORE UPDATE ON evaluacion FOR EACH ROW
BEGIN
    IF NEW.id_intento <> OLD.id_intento OR NEW.calificacion <> OLD.calificacion
        OR NEW.fecha_evaluacion <> OLD.fecha_evaluacion THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La evaluacion consolidada es fija';
    END IF;
END$$

CREATE TRIGGER integridad_detalle_evaluacion_insertar
BEFORE INSERT ON detalle_evaluacion FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM intento_pregunta ip
        LEFT JOIN respuesta r ON r.id_intento = ip.id_intento AND r.id_pregunta = ip.id_pregunta
        LEFT JOIN opcion_respuesta o ON o.id_opcion = r.id_opcion AND o.id_pregunta = r.id_pregunta
        WHERE ip.id_intento = NEW.id_intento AND ip.id_pregunta = NEW.id_pregunta
          AND (NEW.id_opcion <=> r.id_opcion) AND NEW.contestada = (r.id_opcion IS NOT NULL)
          AND NEW.correcta = COALESCE(o.es_correcta, 0)
          AND (NEW.correcta = 1 OR NEW.puntos = 0)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El detalle debe coincidir con la pregunta y la respuesta registrada';
    END IF;
END$$

CREATE TRIGGER integridad_detalle_evaluacion_actualizar
BEFORE UPDATE ON detalle_evaluacion FOR EACH ROW
BEGIN
    IF NEW.id_intento <> OLD.id_intento OR NEW.id_pregunta <> OLD.id_pregunta
        OR NOT (NEW.id_opcion <=> OLD.id_opcion) OR NEW.contestada <> OLD.contestada
        OR NEW.correcta <> OLD.correcta OR NEW.puntos <> OLD.puntos
        OR BINARY NEW.retroalimentacion <> BINARY OLD.retroalimentacion THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El detalle de la evaluacion consolidada es fijo';
    END IF;
END$$

DELIMITER ;
