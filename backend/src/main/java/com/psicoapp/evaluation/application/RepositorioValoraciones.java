package com.psicoapp.evaluation.application;

import java.time.Instant;
import java.util.*;

public interface RepositorioValoraciones {
    record Valoracion(UUID id, UUID idIntento, UUID idDocente, String mensaje, Instant fecha) { }
    void guardar(Valoracion valoracion);
    List<Valoracion> listar(UUID idIntento);
}
