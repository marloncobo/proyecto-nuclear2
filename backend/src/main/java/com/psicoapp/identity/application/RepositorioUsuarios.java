package com.psicoapp.identity.application;

import com.psicoapp.identity.domain.Usuario;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioUsuarios {
    Optional<Usuario> buscarPorId(UUID id);
    Optional<Usuario> buscarPorCorreo(String correo);
    void guardar(Usuario usuario);
}
