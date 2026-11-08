package com.psicoapp.cases.application;

import com.psicoapp.cases.domain.*;
import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.NoEncontrado;
import com.psicoapp.shared.exceptions.Conflicto;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioCreacionCaso {
    public record DatosCreacionCaso(String nombre, String desarrolloSituacional, String contexto,
            Integer tiempoEstimadoMin, List<Escenario> escenarios, Rubrica rubrica) { }

    public record DatosCaso(UUID id, UUID idDocente, String nombre, String desarrolloSituacional, String contexto,
            Integer tiempoEstimadoMin, String estado, List<Escenario> escenarios, Rubrica rubrica) { }

    private final DirectorioIdentidad identidades;
    private final RepositorioCasos casos;

    public ServicioCreacionCaso(DirectorioIdentidad identidades, RepositorioCasos casos) {
        this.identidades = identidades;
        this.casos = casos;
    }

    public DatosCaso crear(UUID idDocente, DatosCreacionCaso datos) {
        identidades.exigirCreacionCasos(idDocente);
        if (datos == null || datos.escenarios() == null) {
            throw new IllegalArgumentException("Los datos y escenarios son obligatorios");
        }
        ConstructorCaso constructor = new ConstructorCasoAcademico(UUID.randomUUID(), idDocente)
                .definirDatos(datos.nombre(), datos.desarrolloSituacional(), datos.contexto(), datos.tiempoEstimadoMin());
        datos.escenarios().forEach(constructor::agregarEscenario);
        Caso caso = constructor.asociarRubrica(datos.rubrica()).construir();
        casos.guardar(caso);
        return representar(caso);
    }

    @Transactional(readOnly = true)
    public DatosCaso consultar(UUID idDocente, UUID idCaso) {
        identidades.exigirDocente(idDocente);
        Caso caso = casos.buscarPorId(idCaso).orElseThrow(() -> new NoEncontrado("Caso no encontrado"));
        if (!caso.idDocente().equals(idDocente)) {
            throw new AccesoDenegado("Solo el docente responsable puede consultar el caso");
        }
        return representar(caso);
    }

    /** Sustituye el contenido del borrador completo manteniendo id y propietario. */
    public DatosCaso actualizar(UUID idDocente, UUID idCaso, DatosCreacionCaso datos) {
        identidades.exigirCreacionCasos(idDocente);
        Caso anterior = casos.bloquearPorId(idCaso).orElseThrow(() -> new NoEncontrado("Caso no encontrado"));
        if (!anterior.idDocente().equals(idDocente)) throw new AccesoDenegado("Solo el responsable puede editar el caso");
        if (anterior.estadoCaso() != EstadoCaso.BORRADOR) throw new Conflicto("Solo puede editar borradores");
        if (datos == null || datos.escenarios() == null) throw new IllegalArgumentException("Los datos y escenarios son obligatorios");
        ConstructorCaso constructor = new ConstructorCasoAcademico(idCaso, idDocente)
                .definirDatos(datos.nombre(), datos.desarrolloSituacional(), datos.contexto(), datos.tiempoEstimadoMin());
        datos.escenarios().forEach(constructor::agregarEscenario);
        Caso actualizado = constructor.asociarRubrica(datos.rubrica()).construir();
        casos.actualizarBorrador(actualizado);
        return representar(actualizado);
    }

    private DatosCaso representar(Caso caso) {
        return new DatosCaso(caso.id(), caso.idDocente(), caso.nombre(), caso.desarrolloSituacional(),
                caso.contexto(), caso.tiempoEstimadoMin(), caso.estado(), caso.escenarios(), caso.rubrica());
    }
}
