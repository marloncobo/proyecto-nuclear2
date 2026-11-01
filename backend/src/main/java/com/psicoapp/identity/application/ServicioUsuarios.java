package com.psicoapp.identity.application;

import com.psicoapp.identity.domain.Rol;
import com.psicoapp.identity.domain.Usuario;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.Conflicto;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioUsuarios implements DirectorioIdentidad {
    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder claves;
    private final PermisosRol permisos;
    private final Set<String> dominiosInstitucionales;

    public ServicioUsuarios(RepositorioUsuarios usuarios, PasswordEncoder claves, PermisosRol permisos,
            @Value("${psicoapp.identidad.dominios-institucionales}") String dominios) {
        this.usuarios = usuarios;
        this.claves = claves;
        this.permisos = permisos;
        this.dominiosInstitucionales = Arrays.stream(dominios.split(","))
                .map(String::strip).filter(s -> !s.isEmpty()).map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public DatosUsuario registrarDocente(UUID idActor, String correo, String nombre, String clave) {
        exigirRol(idActor, Rol.ADMINISTRADOR);
        return crearUsuario(correo, nombre, Rol.DOCENTE, clave);
    }

    public DatosUsuario cambiarRol(UUID idActor, UUID idUsuario, Rol rol) {
        exigirRol(idActor, Rol.ADMINISTRADOR);
        if (idActor.equals(idUsuario) && rol != Rol.ADMINISTRADOR) {
            throw new Conflicto("No puede retirar su propio rol de administrador");
        }
        Usuario actualizado = buscarUsuario(idUsuario).conRol(rol);
        if (rol == Rol.ESTUDIANTE) {
            validarCorreoInstitucional(actualizado.correo());
        }
        usuarios.guardar(actualizado);
        return representarUsuario(actualizado);
    }

    public void configurarCreacionCasos(UUID idActor, Rol rol, boolean habilitado) {
        exigirRol(idActor, Rol.ADMINISTRADOR);
        if (rol != Rol.DOCENTE) {
            throw new IllegalArgumentException("El permiso de creación de casos corresponde al rol docente");
        }
        permisos.configurarCreacionCasos(rol, habilitado);
    }

    @Override
    public DatosUsuario registrarEstudiante(UUID idDocente, String correo, String nombre) {
        exigirRol(idDocente, Rol.DOCENTE);
        String normalizado = validarCorreoInstitucional(correo);
        return usuarios.buscarPorCorreo(normalizado).map(existente -> {
            if (!existente.activo() || existente.rol() != Rol.ESTUDIANTE) {
                throw new Conflicto("El correo corresponde a una cuenta inactiva o de personal del sistema");
            }
            return representarUsuario(existente);
        }).orElseGet(() -> crearUsuario(normalizado, nombre, Rol.ESTUDIANTE, null));
    }

    @Override
    @Transactional(readOnly = true)
    public DatosUsuario exigirDocente(UUID idUsuario) {
        return exigirRol(idUsuario, Rol.DOCENTE);
    }

    @Transactional(readOnly = true)
    public DatosUsuario exigirRol(UUID idUsuario, Rol rol) {
        Usuario usuario = buscarUsuario(idUsuario);
        if (!usuario.activo() || usuario.rol() != rol) {
            throw new AccesoDenegado("El rol no permite esta operación");
        }
        return representarUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public DatosUsuario consultarIdentidad(UUID idUsuario) {
        return representarUsuario(buscarUsuario(idUsuario));
    }

    @Transactional(readOnly = true)
    public void exigirCreacionCasos(UUID idUsuario) {
        DatosUsuario usuario = exigirRol(idUsuario, Rol.DOCENTE);
        if (!usuario.puedeCrearCasos()) {
            throw new AccesoDenegado("El docente no tiene permiso para crear casos");
        }
    }

    public void inicializarAdministrador(String correo, String clave) {
        if (correo.isBlank() && clave.isBlank()) {
            return;
        }
        String normalizado = Usuario.normalizarCorreo(correo);
        var existente = usuarios.buscarPorCorreo(normalizado);
        if (existente.isEmpty()) {
            crearUsuario(normalizado, "Administrador", Rol.ADMINISTRADOR, clave);
        } else if (!existente.get().activo() || existente.get().rol() != Rol.ADMINISTRADOR) {
            throw new Conflicto("El correo de arranque ya pertenece a una cuenta que no es administrador activo");
        }
    }

    private DatosUsuario crearUsuario(String correo, String nombre, Rol rol, String clave) {
        String normalizado = Usuario.normalizarCorreo(correo);
        if (usuarios.buscarPorCorreo(normalizado).isPresent()) {
            throw new Conflicto("El correo ya está registrado");
        }
        String resumen = null;
        if (rol != Rol.ESTUDIANTE) {
            if (clave == null || clave.length() < 8 || clave.length() > 72
                    || clave.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres y hasta 72 bytes");
            }
            resumen = claves.encode(clave);
        }
        Usuario usuario = new Usuario(UUID.randomUUID(), normalizado, nombre, rol, resumen, true, java.time.Instant.now());
        usuarios.guardar(usuario);
        return representarUsuario(usuario);
    }

    private String validarCorreoInstitucional(String correo) {
        String normalizado = Usuario.normalizarCorreo(correo);
        if (!dominiosInstitucionales.contains(normalizado.substring(normalizado.indexOf('@') + 1))) {
            throw new IllegalArgumentException("El correo debe pertenecer a un dominio institucional configurado");
        }
        return normalizado;
    }

    private Usuario buscarUsuario(UUID id) {
        return usuarios.buscarPorId(id).orElseThrow(() -> new NoEncontrado("Usuario no encontrado"));
    }

    private DatosUsuario representarUsuario(Usuario usuario) {
        return new DatosUsuario(usuario.id(), usuario.correo(), usuario.nombre(), usuario.rol().name(),
                usuario.rol() == Rol.DOCENTE && permisos.puedeCrearCasos(usuario.rol()));
    }
}
