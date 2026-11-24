SET @migracion_clave_inicial = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'usuario'
          AND COLUMN_NAME = 'clave_inicial'
    ),
    'DO 0',
    'ALTER TABLE usuario ADD COLUMN clave_inicial BOOLEAN NOT NULL DEFAULT FALSE'
);
PREPARE migracion_clave_inicial FROM @migracion_clave_inicial;
EXECUTE migracion_clave_inicial;
DEALLOCATE PREPARE migracion_clave_inicial;
