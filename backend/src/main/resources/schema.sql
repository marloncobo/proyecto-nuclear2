-- MySQL 8.0+. Los UUID del ModeloER.drawio se representan mediante CHAR(36).
CREATE TABLE IF NOT EXISTS rol (
    id_rol CHAR(36) PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(200) NOT NULL
);
CREATE TABLE IF NOT EXISTS permiso (
    id_permiso CHAR(36) PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    descripcion VARCHAR(200) NOT NULL
);
CREATE TABLE IF NOT EXISTS rol_permiso (
    id_rol CHAR(36) NOT NULL,
    id_permiso CHAR(36) NOT NULL,
    PRIMARY KEY (id_rol, id_permiso),
    FOREIGN KEY (id_rol) REFERENCES rol(id_rol),
    FOREIGN KEY (id_permiso) REFERENCES permiso(id_permiso)
);
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario CHAR(36) PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(180) NOT NULL UNIQUE,
    contrasena_hash VARCHAR(255) NULL,
    activo BOOLEAN NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    id_rol CHAR(36) NOT NULL,
    FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
);
CREATE TABLE IF NOT EXISTS espacio_academico (
    id_espacio CHAR(36) PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    periodo VARCHAR(30) NOT NULL
);
INSERT INTO rol (id_rol, nombre, descripcion)
SELECT '00000000-0000-0000-0000-000000000001', 'ADMINISTRADOR', 'Administra usuarios, roles y permisos'
WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre = 'ADMINISTRADOR');
INSERT INTO rol (id_rol, nombre, descripcion)
SELECT '00000000-0000-0000-0000-000000000002', 'DOCENTE', 'Organiza actividades academicas'
WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre = 'DOCENTE');
INSERT INTO rol (id_rol, nombre, descripcion)
SELECT '00000000-0000-0000-0000-000000000003', 'ESTUDIANTE', 'Participa en convocatorias autorizadas'
WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre = 'ESTUDIANTE');
INSERT INTO permiso (id_permiso, nombre, descripcion)
SELECT '00000000-0000-0000-0000-000000000004', 'CREAR_CASOS', 'Permite al rol docente crear casos'
WHERE NOT EXISTS (SELECT 1 FROM permiso WHERE nombre = 'CREAR_CASOS');

-- Construccion de casos (Builder). Columnas y relaciones del ModeloER.drawio.
-- El tiempo puede omitirse en un borrador; publicar exigira completarlo.
CREATE TABLE IF NOT EXISTS caso (
    id_caso CHAR(36) PRIMARY KEY,
    id_usuario_docente CHAR(36) NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    desarrollo_situacional TEXT NOT NULL,
    contexto TEXT NOT NULL,
    tiempo_estimado_min INT NULL,
    estado VARCHAR(20) NOT NULL,
    FOREIGN KEY (id_usuario_docente) REFERENCES usuario(id_usuario),
    CHECK (tiempo_estimado_min IS NULL OR tiempo_estimado_min > 0),
    CHECK (estado IN ('BORRADOR', 'PUBLICADO', 'ARCHIVADO'))
);
CREATE TABLE IF NOT EXISTS escenario (
    id_escenario CHAR(36) PRIMARY KEY,
    id_caso CHAR(36) NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    contexto_situacional TEXT NOT NULL,
    orden INT NOT NULL,
    UNIQUE (id_caso, orden),
    FOREIGN KEY (id_caso) REFERENCES caso(id_caso),
    CHECK (orden > 0)
);
CREATE TABLE IF NOT EXISTS pregunta (
    id_pregunta CHAR(36) PRIMARY KEY,
    id_escenario CHAR(36) NOT NULL,
    enunciado TEXT NOT NULL,
    orden INT NOT NULL,
    UNIQUE (id_escenario, orden),
    FOREIGN KEY (id_escenario) REFERENCES escenario(id_escenario),
    CHECK (orden > 0)
);
CREATE TABLE IF NOT EXISTS opcion_respuesta (
    id_opcion CHAR(36) PRIMARY KEY,
    id_pregunta CHAR(36) NOT NULL,
    texto TEXT NOT NULL,
    es_correcta BOOLEAN NOT NULL,
    justificacion TEXT NOT NULL,
    FOREIGN KEY (id_pregunta) REFERENCES pregunta(id_pregunta)
);
CREATE TABLE IF NOT EXISTS rubrica (
    id_rubrica CHAR(36) PRIMARY KEY,
    id_caso CHAR(36) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    escala_maxima DECIMAL(5,2) NOT NULL,
    FOREIGN KEY (id_caso) REFERENCES caso(id_caso),
    CHECK (escala_maxima > 0)
);
CREATE TABLE IF NOT EXISTS criterio_rubrica (
    id_criterio CHAR(36) PRIMARY KEY,
    id_rubrica CHAR(36) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    ponderacion DECIMAL(5,2) NOT NULL,
    FOREIGN KEY (id_rubrica) REFERENCES rubrica(id_rubrica),
    CHECK (ponderacion >= 0)
);
CREATE TABLE IF NOT EXISTS criterio_pregunta (
    id_criterio CHAR(36) NOT NULL,
    id_pregunta CHAR(36) NOT NULL,
    PRIMARY KEY (id_criterio, id_pregunta),
    FOREIGN KEY (id_criterio) REFERENCES criterio_rubrica(id_criterio),
    FOREIGN KEY (id_pregunta) REFERENCES pregunta(id_pregunta)
);
