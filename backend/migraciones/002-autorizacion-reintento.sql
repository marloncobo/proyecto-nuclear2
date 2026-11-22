ALTER TABLE intento MODIFY COLUMN fecha_inicio TIMESTAMP(6) NOT NULL,
    MODIFY COLUMN fecha_limite TIMESTAMP(6) NOT NULL,
    ADD INDEX indice_estudiante_intento_activo (id_estudiante, estado);

CREATE TABLE IF NOT EXISTS autorizacion_reintento (
    id_intento CHAR(36) PRIMARY KEY,
    id_docente CHAR(36) NOT NULL,
    fecha_autorizacion TIMESTAMP NOT NULL,
    FOREIGN KEY (id_intento) REFERENCES intento(id_intento),
    FOREIGN KEY (id_docente) REFERENCES usuario(id_usuario)
);
