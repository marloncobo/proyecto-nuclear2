CREATE TABLE IF NOT EXISTS recuperacion_clave (
    id_usuario CHAR(36) PRIMARY KEY,
    resumen CHAR(64) NOT NULL,
    vence TIMESTAMP NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
);
