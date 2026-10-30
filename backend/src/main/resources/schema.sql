-- MySQL 8.0+. UUIDs from ModeloER.drawio are represented as CHAR(36).
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