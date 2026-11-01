package com.psicoapp.cases.domain;

public interface ConstructorCaso {
    ConstructorCaso definirDatos(String nombre, String desarrolloSituacional, String contexto, Integer tiempoEstimadoMin);
    ConstructorCaso agregarEscenario(Escenario escenario);
    ConstructorCaso asociarRubrica(Rubrica rubrica);
    Caso construir();
}
