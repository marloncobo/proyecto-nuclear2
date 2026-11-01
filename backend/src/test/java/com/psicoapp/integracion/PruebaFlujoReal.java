package com.psicoapp.integracion;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flujo-real;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "psicoapp.identidad.dominios-institucionales=example.test",
        "psicoapp.identidad.correo-administrador=", "psicoapp.identidad.clave-administrador=",
        "psicoapp.sesiones.cierre-automatico-habilitado=false"
})
@AutoConfigureMockMvc
class PruebaFlujoReal extends FlujoBackend { }
