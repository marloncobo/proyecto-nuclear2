package com.psicoapp.identity.infrastructure;

import java.sql.DriverManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PruebaMigracionPermisoCreacionCasos {
    @Test
    void migraElPermisoAnteriorUnaVezYRespetaLasRevocaciones() throws Exception {
        var esquema = new ClassPathResource("schema.sql");
        try (var conexion = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
                var sql = conexion.createStatement()) {
            ScriptUtils.executeSqlScript(conexion, esquema);
            sql.executeUpdate("""
                    INSERT INTO usuario (id_usuario,nombre,correo,activo,fecha_creacion,id_rol)
                    SELECT 'docente-a','Docente A','a@example.test',TRUE,CURRENT_TIMESTAMP,id_rol
                    FROM rol WHERE nombre = 'DOCENTE'
                    UNION ALL
                    SELECT 'docente-b','Docente B','b@example.test',TRUE,CURRENT_TIMESTAMP,id_rol
                    FROM rol WHERE nombre = 'DOCENTE'
                    UNION ALL
                    SELECT 'estudiante','Estudiante','estudiante@example.test',TRUE,CURRENT_TIMESTAMP,id_rol
                    FROM rol WHERE nombre = 'ESTUDIANTE'
                    """);
            sql.executeUpdate("""
                    INSERT INTO rol_permiso (id_rol,id_permiso)
                    SELECT r.id_rol,p.id_permiso FROM rol r CROSS JOIN permiso p
                    WHERE r.nombre = 'DOCENTE' AND p.nombre = 'CREAR_CASOS'
                    """);
            ScriptUtils.executeSqlScript(conexion, esquema);
            try (var filas = sql.executeQuery("SELECT id_usuario FROM permiso_creacion_casos ORDER BY id_usuario")) {
                filas.next();
                assertEquals("docente-a", filas.getString(1));
                filas.next();
                assertEquals("docente-b", filas.getString(1));
                assertFalse(filas.next());
            }
            sql.executeUpdate("DELETE FROM permiso_creacion_casos WHERE id_usuario = 'docente-a'");
            sql.executeUpdate("""
                    INSERT INTO usuario (id_usuario,nombre,correo,activo,fecha_creacion,id_rol)
                    SELECT 'docente-nuevo','Docente nuevo','nuevo@example.test',TRUE,CURRENT_TIMESTAMP,id_rol
                    FROM rol WHERE nombre = 'DOCENTE'
                    """);
            ScriptUtils.executeSqlScript(conexion, esquema);
            try (var filas = sql.executeQuery("SELECT id_usuario FROM permiso_creacion_casos")) {
                filas.next();
                assertEquals("docente-b", filas.getString(1));
                assertFalse(filas.next());
            }
        }
    }
}
