package com.psicoapp.cases.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Una instancia por construccion; sin persistencia, autorizacion ni publicacion. */
public final class ConstructorCasoAcademico implements ConstructorCaso {
    private final UUID id;
    private final UUID idDocente;
    private String nombre;
    private String desarrolloSituacional;
    private String contexto;
    private Integer tiempoEstimadoMin;
    private final List<Escenario> escenarios = new ArrayList<>();
    private Rubrica rubrica;

    public ConstructorCasoAcademico(UUID id, UUID idDocente) {
        ContenidoCaso.identificador(id);
        ContenidoCaso.identificador(idDocente);
        this.id = id;
        this.idDocente = idDocente;
    }

    @Override
    public ConstructorCaso definirDatos(String nombre, String desarrolloSituacional, String contexto, Integer tiempoEstimadoMin) {
        this.nombre = nombre;
        this.desarrolloSituacional = desarrolloSituacional;
        this.contexto = contexto;
        this.tiempoEstimadoMin = tiempoEstimadoMin;
        return this;
    }

    @Override
    public ConstructorCaso agregarEscenario(Escenario escenario) {
        if (escenario == null) throw new IllegalArgumentException("El escenario es obligatorio");
        escenarios.add(escenario);
        return this;
    }

    @Override
    public ConstructorCaso asociarRubrica(Rubrica rubrica) {
        this.rubrica = rubrica;
        return this;
    }

    @Override
    public Caso construir() {
        return new Caso(id, idDocente, nombre, desarrolloSituacional, contexto, tiempoEstimadoMin, escenarios, rubrica);
    }
}
