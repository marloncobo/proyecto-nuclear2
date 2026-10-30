package com.psicoapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "psicoapp.identidad.correo-administrador=",
        "psicoapp.identidad.clave-administrador="
})
class PsicoappApplicationTests {

	@Test
	void contextLoads() {
	}

}
