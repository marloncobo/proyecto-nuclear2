package com.psicoapp.identity.application;

import com.psicoapp.identity.domain.Usuario;
import com.psicoapp.identity.domain.Rol;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioUsuarios {
    List<Usuario> listar(FiltroUsuarios filtro);
    default List<Usuario> listar(Rol rol, Boolean activo) {
        return listar(new FiltroUsuarios(rol, activo, null));
    }
    void actualizarDatos(UUID id, Rol rolEsperado, String correo, String nombre);
    void actualizarEstado(UUID id, boolean activo);
    Optional<Usuario> buscarPorId(UUID id);
    Optional<Usuario> buscarPorCorreo(String correo);
    Optional<Usuario> bloquear(UUID id);
    void restablecerClave(UUID id, String hashNuevo);
    boolean actualizarClaveEstudiante(UUID id, String hashAnterior, String hashNuevo, boolean inicial);
    boolean cambiarClave(UUID id, String hashAnterior, String hashNuevo);
    void guardar(Usuario usuario);
}
