package com.psicoapp.notifications.application;

import java.util.List;
import java.util.UUID;
import com.psicoapp.shared.exceptions.NoEncontrado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServicioAvisos {
    private final RepositorioAvisos avisos;
    public ServicioAvisos(RepositorioAvisos avisos) { this.avisos = avisos; }
    public List<RepositorioAvisos.Aviso> listar(UUID usuario, int limite, int desplazamiento) {
        if (limite < 1 || limite > 100 || desplazamiento < 0) throw new IllegalArgumentException("Paginacion invalida");
        return avisos.listar(usuario, limite, desplazamiento);
    }
    @Transactional
    public void marcarLeida(UUID usuario, UUID aviso) {
        if (!avisos.marcarLeida(usuario, aviso)) throw new NoEncontrado("Aviso no encontrado");
    }
}
