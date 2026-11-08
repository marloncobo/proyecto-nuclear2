package com.psicoapp.cases.application;

import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.Conflicto;
import com.psicoapp.shared.exceptions.NoEncontrado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional
public class ServicioConsultaCasos implements CatalogoCasos {
    private final RepositorioCasos casos;
    private final DirectorioIdentidad identidades;

    public ServicioConsultaCasos(RepositorioCasos casos, DirectorioIdentidad identidades) {
        this.casos = casos;
        this.identidades = identidades;
    }

    @Override
    public String consultarNombre(UUID idDocente, UUID idCaso) {
        identidades.exigirDocente(idDocente);
        var caso = casos.buscarPorId(idCaso).orElseThrow(() -> new NoEncontrado("Caso no encontrado"));
        if (!caso.idDocente().equals(idDocente)) throw new AccesoDenegado("Solo el docente responsable puede consultar el caso");
        return caso.nombre();
    }

    @Override
    public CasoProgramable exigirPublicado(UUID idDocente, UUID idCaso) {
        identidades.exigirDocente(idDocente);
        var caso = casos.bloquearPorId(idCaso).orElseThrow(() -> new NoEncontrado("Caso no encontrado"));
        if (!caso.idDocente().equals(idDocente)) {
            throw new AccesoDenegado("Solo el docente responsable puede programar el caso");
        }
        if (!"PUBLICADO".equals(caso.estado()) || caso.tiempoEstimadoMin() == null) {
            throw new Conflicto("Solo se pueden programar casos publicados con tiempo estimado");
        }
        return new CasoProgramable(caso.id(), caso.tiempoEstimadoMin(), caso.nombre());
    }
}
