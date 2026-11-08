package com.psicoapp.notifications;

import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Reutiliza el recorrido completo exclusivamente en un esquema temporal propio. */
@EnabledIfEnvironmentVariable(named = "PSICOAPP_PRUEBAS_MYSQL_URL", matches = "jdbc:mysql://.+")
class PruebaFlujosNotificacionesMySql extends PruebaFlujosNotificaciones {
    private static final String ESQUEMA = "psicoapp_notificaciones_prueba_" + UUID.randomUUID().toString().replace("-", "");
    private static String servidor;
    private static boolean creado;
    @DynamicPropertySource
    static void prepararBase(DynamicPropertyRegistry propiedades) throws Exception {
        servidor = System.getenv("PSICOAPP_PRUEBAS_MYSQL_URL");
        if (servidor == null || !servidor.matches("jdbc:mysql://[A-Za-z0-9.:-]+/"))
            throw new IllegalArgumentException("Configure una URL de servidor MySQL sin nombre de base");
        if (usuario() == null || clave() == null) throw new IllegalStateException("Configure credenciales de MySQL para pruebas");
        try (var conexion = DriverManager.getConnection(servidor, usuario(), clave());
                var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("CREATE DATABASE " + ESQUEMA + " CHARACTER SET utf8mb4");
            creado = true;
        }
        propiedades.add("spring.datasource.url", () -> servidor + ESQUEMA
                + "?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true");
        propiedades.add("spring.datasource.username", PruebaFlujosNotificacionesMySql::usuario);
        propiedades.add("spring.datasource.password", PruebaFlujosNotificacionesMySql::clave);
    }
    @AfterAll
    static void eliminarSoloEsquemaPropio() throws Exception {
        if (!creado || !ESQUEMA.matches("psicoapp_notificaciones_prueba_[a-f0-9]{32}")) return;
        try (var conexion = DriverManager.getConnection(servidor, usuario(), clave());
                var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("DROP DATABASE " + ESQUEMA);
        }
    }
    private static String usuario() { return System.getenv("PSICOAPP_PRUEBAS_MYSQL_USUARIO"); }
    private static String clave() { return System.getenv("PSICOAPP_PRUEBAS_MYSQL_CLAVE"); }
}
