package com.psicoapp.sessions.infrastructure;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ConfiguracionSesiones {
    @Bean
    Clock relojSesiones() {
        return Clock.systemUTC();
    }
}
