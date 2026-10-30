package com.psicoapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "psicoapp.identity.admin-email=",
        "psicoapp.identity.admin-password="
})
class PsicoappApplicationTests {

	@Test
	void contextLoads() {
	}

}
