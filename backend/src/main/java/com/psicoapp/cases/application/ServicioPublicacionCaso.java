package com.psicoapp.cases.application;

import com.psicoapp.cases.domain.Caso;
import com.psicoapp.cases.domain.EstadoCaso;
import com.psicoapp.cases.domain.ResultadoValidacion;
import com.psicoapp.cases.domain.ValidacionCasoAcademico;
import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.Conflicto;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioPublicacionCaso {
    public record DatosPublicacion(UUID id, String estado) { }

    private final DirectorioIdentidad identidades;
    private final RepositorioCasos casos;

    public ServicioPublicacionCaso(DirectorioIdentidad identidades, RepositorioCasos casos) {
        this.identidades = identidades;
        this.casos = casos;
    }

    public DatosPublicacion publicar(UUID idDocente, UUID idCaso) {
        identidades.exigirCreacionCasos(idDocente);
        Caso caso = casos.buscarPorId(idCaso).orElseThrow(() -> new NoEncontrado("Caso no encontrado"));
        if (!caso.idDocente().equals(idDocente)) {
            throw new AccesoDenegado("Solo el docente responsable puede publicar el caso");
        }
        if (caso.estadoCaso() != EstadoCaso.BORRADOR) {
            throw new Conflicto("Solo puede publicar un caso en borrador");
        }
        ResultadoValidacion resultado = new ValidacionCasoAcademico().validar(caso);
        if (!resultado.esValido()) throw new PublicacionInvalida(resultado);
        if (!casos.publicar(idCaso, idDocente)) {
            throw new Conflicto("El estado del caso cambio durante la publicacion");
        }
        return new DatosPublicacion(idCaso, EstadoCaso.PUBLICADO.name());
    }
}
