package com.psicoapp.evaluation.infrastructure;

import com.psicoapp.evaluation.domain.CalificacionPorPonderacion;
import com.psicoapp.evaluation.domain.EstrategiaCalificacion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionEvaluacion {
    @Bean
    EstrategiaCalificacion estrategiaCalificacion() {
        return new CalificacionPorPonderacion();
    }
}
