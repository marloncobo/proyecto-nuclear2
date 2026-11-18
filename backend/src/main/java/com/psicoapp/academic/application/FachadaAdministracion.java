package com.psicoapp.academic.application;

import com.psicoapp.academic.domain.EspacioAcademico;
import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.shared.exceptions.NoEncontrado;
import com.psicoapp.shared.exceptions.Conflicto;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/** RF-04: fachada acotada que coordina autorizacion y espacios academicos. */
@Service
@Transactional
public class FachadaAdministracion {
    public record DatosEspacioAcademico(UUID id, String nombre, String periodo) { }

    private final RepositorioEspaciosAcademicos espacios;
    private final DirectorioIdentidad identidades;
    private final ConsultaUsoEspacios usoEspacios;

    public FachadaAdministracion(RepositorioEspaciosAcademicos espacios, DirectorioIdentidad identidades,
            ConsultaUsoEspacios usoEspacios) {
        this.espacios = espacios;
        this.identidades = identidades;
        this.usoEspacios = usoEspacios;
    }

    /** Mantiene el bloqueo hasta que la transaccion de programacion termine. */
    @Transactional(propagation = Propagation.MANDATORY)
    public DatosEspacioAcademico bloquearParaProgramacion(UUID idDocente, UUID id) {
        identidades.exigirDocente(idDocente);
        return representar(espacios.bloquearPorId(id)
                .orElseThrow(() -> new NoEncontrado("Espacio academico no encontrado")));
    }

    public DatosEspacioAcademico editarEspacio(UUID idDocente, UUID id, String nombre, String periodo) {
        identidades.exigirDocente(idDocente);
        EspacioAcademico anterior = espacios.bloquearPorId(id)
                .orElseThrow(() -> new NoEncontrado("Espacio academico no encontrado"));
        if (usoEspacios.tieneSesiones(id)) {
            throw new Conflicto("El espacio ya esta asociado a una sesion");
        }
        EspacioAcademico actualizado = new EspacioAcademico(anterior.id(), nombre, periodo);
        espacios.actualizar(actualizado);
        return representar(actualizado);
    }

    public DatosEspacioAcademico crearEspacio(UUID idDocente, String nombre, String periodo) {
        identidades.exigirDocente(idDocente);
        EspacioAcademico espacio = new EspacioAcademico(UUID.randomUUID(), nombre, periodo);
        espacios.guardar(espacio);
        return representar(espacio);
    }

    @Transactional(readOnly = true)
    public DatosEspacioAcademico consultarEspacio(UUID idDocente, UUID id) {
        identidades.exigirDocente(idDocente);
        return espacios.buscarPorId(id).map(this::representar)
                .orElseThrow(() -> new NoEncontrado("Espacio academico no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<DatosEspacioAcademico> listarEspacios(UUID idDocente) {
        identidades.exigirDocente(idDocente);
        return espacios.listarTodos().stream().map(this::representar).toList();
    }

    private DatosEspacioAcademico representar(EspacioAcademico espacio) {
        return new DatosEspacioAcademico(espacio.id(), espacio.nombre(), espacio.periodo());
    }
}
