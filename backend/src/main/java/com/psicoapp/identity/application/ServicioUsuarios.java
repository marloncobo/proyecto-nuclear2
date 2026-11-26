package com.psicoapp.identity.application;

import com.psicoapp.identity.domain.Rol;
import com.psicoapp.identity.domain.Usuario;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.Conflicto;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.util.Arrays;
import java.util.Locale;
import java.util.List;
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
    public record DatosUsuarioAdministrado(UUID id, String correo, String nombre, String rol,
            boolean puedeCrearCasos, boolean activo) { }

    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder claves;
    private final PermisosUsuario permisos;
    private final Set<String> dominiosInstitucionales;

    public ServicioUsuarios(RepositorioUsuarios usuarios, PasswordEncoder claves, PermisosUsuario permisos,
            @Value("${psicoapp.identidad.dominios-institucionales}") String dominios) {
        this.usuarios = usuarios;
        this.claves = claves;
        this.permisos = permisos;
        this.dominiosInstitucionales = Arrays.stream(dominios.split(","))
                .map(String::strip).filter(s -> !s.isEmpty()).map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public List<DatosUsuarioAdministrado> listarUsuarios(UUID idActor, Rol rol, Boolean activo) {
        return listarUsuarios(idActor, rol, activo, null);
    }

    @Transactional(readOnly = true)
    public List<DatosUsuarioAdministrado> listarUsuarios(UUID idActor, Rol rol, Boolean activo, String busqueda) {
        exigirRol(idActor, Rol.ADMINISTRADOR);
        return usuarios.listar(new FiltroUsuarios(rol, activo, busqueda)).stream()
                .map(this::representarAdministrado).toList();
    }

    @Transactional(readOnly = true)
    public DatosUsuarioAdministrado consultarUsuario(UUID idActor, UUID idUsuario) {
        exigirRol(idActor, Rol.ADMINISTRADOR);
        return representarAdministrado(buscarUsuario(idUsuario));
    }

    public DatosUsuarioAdministrado editarDocente(UUID idActor, UUID idUsuario, String correo, String nombre) {
        exigirRol(idActor, Rol.ADMINISTRADOR);
        Usuario anterior = buscarUsuario(idUsuario);
        if (anterior.rol() != Rol.DOCENTE) throw new Conflicto("La cuenta no tiene rol docente");
        String normalizado = validarCorreoInstitucional(correo);
        if (usuarios.buscarPorCorreo(normalizado).filter(u -> !u.id().equals(idUsuario)).isPresent()) {
            throw new Conflicto("El correo ya esta registrado");
        }
        Usuario actualizado = new Usuario(anterior.id(), normalizado, nombre, anterior.rol(), anterior.hashClave(),
                anterior.activo(), anterior.fechaCreacion(), anterior.claveInicial());
        usuarios.actualizarDatosDocente(idUsuario, actualizado.correo(), actualizado.nombre());
        return representarAdministrado(actualizado);
    }

    public DatosUsuarioAdministrado cambiarEstado(UUID idActor, UUID idUsuario, boolean activo) {
        exigirRol(idActor, Rol.ADMINISTRADOR);
        if (idActor.equals(idUsuario) && !activo) throw new Conflicto("No puede desactivar su propia cuenta");
        Usuario anterior = buscarUsuario(idUsuario);
        Usuario actualizado = new Usuario(anterior.id(), anterior.correo(), anterior.nombre(), anterior.rol(),
                anterior.hashClave(), activo, anterior.fechaCreacion(), anterior.claveInicial());
        usuarios.actualizarEstado(idUsuario, activo);
        return representarAdministrado(actualizado);
    }

    @Transactional(readOnly = true)
    public List<Rol> listarRoles(UUID idActor) {
        exigirRol(idActor, Rol.ADMINISTRADOR);
        return List.of(Rol.values());
    }

    @Transactional(readOnly = true)
    public DatosUsuario buscarEstudiante(UUID idActor, String correo) {
        exigirGestorEstudiantes(idActor);
        return usuarios.buscarPorCorreo(validarCorreoInstitucional(correo))
                .filter(u -> u.activo() && u.rol() == Rol.ESTUDIANTE).map(this::representarUsuario)
                .orElseThrow(() -> new NoEncontrado("Estudiante no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<DatosUsuario> listarEstudiantes(UUID idActor) {
        exigirGestorEstudiantes(idActor);
        return usuarios.listar(Rol.ESTUDIANTE, true).stream()
                .filter(usuario -> dominioInstitucional(usuario.correo()))
                .map(this::representarUsuario).toList();
    }

    private DatosUsuarioAdministrado representarAdministrado(Usuario usuario) {
        DatosUsuario datos = representarUsuario(usuario);
        return new DatosUsuarioAdministrado(datos.id(), datos.correo(), datos.nombre(), datos.rol(),
                datos.puedeCrearCasos(), usuario.activo());
    }
    public DatosUsuario registrarDocente(UUID idActor, String correo, String nombre, String clave) {
        exigirRol(idActor, Rol.ADMINISTRADOR);
        return crearUsuario(validarCorreoInstitucional(correo), nombre, Rol.DOCENTE, clave);
    }

    public DatosUsuario cambiarRol(UUID idActor, UUID idUsuario, Rol rol) {
        exigirRol(idActor, Rol.ADMINISTRADOR);
        if (idActor.equals(idUsuario) && rol != Rol.ADMINISTRADOR) {
            throw new Conflicto("No puede retirar su propio rol de administrador");
        }
        Usuario actualizado = buscarUsuario(idUsuario).conRol(rol);
        if (rol == Rol.ESTUDIANTE || rol == Rol.DOCENTE) {
            validarCorreoInstitucional(actualizado.correo());
        }
        usuarios.guardar(actualizado);
        if (rol != Rol.DOCENTE) permisos.retirarCreacionCasos(idUsuario);
        return representarUsuario(actualizado);
    }

    public DatosUsuarioAdministrado configurarCreacionCasos(UUID idActor, UUID idUsuario, boolean habilitado) {
        exigirRol(idActor, Rol.ADMINISTRADOR);
        Usuario usuario = buscarUsuario(idUsuario);
        if (usuario.rol() != Rol.DOCENTE) {
            throw new IllegalArgumentException("El permiso de creación de casos solo se concede a docentes");
        }
        permisos.configurarCreacionCasos(idUsuario, habilitado);
        return new DatosUsuarioAdministrado(usuario.id(), usuario.correo(), usuario.nombre(),
                usuario.rol().name(), habilitado, usuario.activo());
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

    @Override
    @Transactional(readOnly = true)
    public void exigirEstudiante(UUID idUsuario) {
        DatosUsuario estudiante = exigirRol(idUsuario, Rol.ESTUDIANTE);
        validarCorreoInstitucional(estudiante.correo());
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

    @Override
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

    public DatosUsuario registrarEstudiante(UUID actor, String correo, String nombre, String clave) {
        exigirGestorEstudiantes(actor);
        return crearUsuario(validarCorreoInstitucional(correo), nombre, Rol.ESTUDIANTE, clave);
    }

    public void asignarClaveInicial(UUID actor, UUID estudiante, String clave) {
        exigirGestorEstudiantes(actor);
        exigirEstudiante(estudiante);
        if (!usuarios.actualizarClaveEstudiante(estudiante, null, codificarClave(clave), true)) {
            throw new Conflicto("La cuenta ya tiene contraseña");
        }
    }

    public void cambiarClaveInicial(UUID estudiante, String clave) {
        cambiarClaveInicial(estudiante, clave, false);
    }

    public void cambiarClaveInicial(UUID estudiante, String clave, boolean conservarClaveInicial) {
        exigirEstudiante(estudiante);
        var cuenta = buscarUsuario(estudiante);
        if (!cuenta.claveInicial() || cuenta.hashClave() == null) {
            throw new AccesoDenegado("La cuenta no tiene un cambio de contraseña pendiente");
        }
        validarClave(clave);
        if (!conservarClaveInicial && claves.matches(clave, cuenta.hashClave())) {
            throw new ConfirmacionClaveInicialRequerida();
        }
        String nuevo = claves.encode(clave);
        if (!usuarios.actualizarClaveEstudiante(estudiante, cuenta.hashClave(), nuevo, false)) {
            throw new Conflicto("La contraseña inicial ya fue cambiada");
        }
    }

    public void cambiarClave(UUID id, String actual, String nueva) {
        Usuario usuario = buscarUsuario(id);
        if (!usuario.activo() || usuario.claveInicial() || usuario.hashClave() == null) {
            throw new AccesoDenegado("La cuenta no puede cambiar la contraseña desde Mi cuenta");
        }
        if (actual == null || actual.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
                || !claves.matches(actual, usuario.hashClave())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
        if (actual.equals(nueva)) {
            throw new IllegalArgumentException("La nueva contraseña debe ser distinta de la actual");
        }
        if (!usuarios.cambiarClave(id, usuario.hashClave(), codificarClave(nueva))) {
            throw new Conflicto("La contraseña cambió. Verifica la contraseña actual e inténtalo de nuevo");
        }
    }

    private void exigirGestorEstudiantes(UUID actor) {
        var usuario = buscarUsuario(actor);
        if (!usuario.activo() || (usuario.rol() != Rol.DOCENTE && usuario.rol() != Rol.ADMINISTRADOR)) {
            throw new AccesoDenegado("El rol no permite gestionar estudiantes");
        }
    }

    private String codificarClave(String clave) {
        validarClave(clave);
        return claves.encode(clave);
    }

    private void validarClave(String clave) {
        if (clave == null || clave.isBlank() || clave.length() < 8
                || clave.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres y hasta 72 bytes");
        }
    }

    private DatosUsuario crearUsuario(String correo, String nombre, Rol rol, String clave) {
        String normalizado = Usuario.normalizarCorreo(correo);
        if (usuarios.buscarPorCorreo(normalizado).isPresent()) {
            throw new Conflicto("El correo ya está registrado");
        }
        boolean inicial = rol == Rol.ESTUDIANTE && clave != null;
        String resumen = rol != Rol.ESTUDIANTE || inicial ? codificarClave(clave) : null;
        Usuario usuario = new Usuario(UUID.randomUUID(), normalizado, nombre, rol, resumen, true,
                java.time.Instant.now(), inicial);
        try {
            usuarios.guardar(usuario);
        } catch (org.springframework.dao.DuplicateKeyException excepcion) {
            throw new Conflicto("El correo ya está registrado");
        }
        return representarUsuario(usuario);
    }

    private String validarCorreoInstitucional(String correo) {
        String normalizado = Usuario.normalizarCorreo(correo);
        if (!dominioInstitucional(normalizado)) {
            throw new IllegalArgumentException("El correo debe pertenecer a un dominio institucional configurado");
        }
        return normalizado;
    }

    private boolean dominioInstitucional(String correo) {
        return dominiosInstitucionales.contains(correo.substring(correo.indexOf('@') + 1));
    }

    private Usuario buscarUsuario(UUID id) {
        return usuarios.buscarPorId(id).orElseThrow(() -> new NoEncontrado("Usuario no encontrado"));
    }

    private DatosUsuario representarUsuario(Usuario usuario) {
        return new DatosUsuario(usuario.id(), usuario.correo(), usuario.nombre(), usuario.rol().name(),
                usuario.rol() == Rol.DOCENTE && permisos.puedeCrearCasos(usuario.id()));
    }
}
