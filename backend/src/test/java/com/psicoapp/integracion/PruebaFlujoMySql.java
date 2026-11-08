package com.psicoapp.integracion;

import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@EnabledIfEnvironmentVariable(named="PSICOAPP_PRUEBAS_MYSQL_URL", matches="jdbc:mysql://.+")
@SpringBootTest(properties = {
        "psicoapp.notificaciones.clave-cifrado=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "psicoapp.notificaciones.despacho-habilitado=false",
        "psicoapp.identidad.dominios-institucionales=example.test",
        "psicoapp.identidad.correo-administrador=", "psicoapp.identidad.clave-administrador=",
        "psicoapp.sesiones.cierre-automatico-habilitado=false"
})
@AutoConfigureMockMvc
class PruebaFlujoMySql extends FlujoBackend {
    private static String base;
    private static final String ESQUEMA = "psicoapp_prueba_" + UUID.randomUUID().toString().replace("-","");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry propiedades) throws Exception {
        base = System.getenv("PSICOAPP_PRUEBAS_MYSQL_URL");
        if (!base.matches("jdbc:mysql://[A-Za-z0-9.:-]+/")) {
            throw new IllegalArgumentException("La URL debe contener solo servidor y puerto, terminar en / y no indicar una base existente");
        }
        try (Connection conexion=DriverManager.getConnection(base,usuario(),clave()); Statement sentencia=conexion.createStatement()) {
            sentencia.executeUpdate("CREATE DATABASE "+ESQUEMA+" CHARACTER SET utf8mb4");
        }
        propiedades.add("spring.datasource.url", () -> base+ESQUEMA+"?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true");
        propiedades.add("spring.datasource.username", PruebaFlujoMySql::usuario);
        propiedades.add("spring.datasource.password", PruebaFlujoMySql::clave);
    }

    @AfterAll
    static void eliminarSoloEsquemaGenerado() throws Exception {
        if (base == null || !ESQUEMA.matches("psicoapp_prueba_[a-f0-9]{32}")) return;
        try (Connection conexion=DriverManager.getConnection(base,usuario(),clave()); Statement sentencia=conexion.createStatement()) {
            sentencia.executeUpdate("DROP DATABASE "+ESQUEMA);
        }
    }

    private static String usuario() { return System.getenv("PSICOAPP_PRUEBAS_MYSQL_USUARIO"); }
    private static String clave() { return System.getenv("PSICOAPP_PRUEBAS_MYSQL_CLAVE"); }
}
