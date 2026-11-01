package com.psicoapp.identity.application;

import java.util.UUID;

/** Contrato publico entre modulos; no expone credenciales ni entidades internas. */
public interface DirectorioIdentidad {
    record DatosUsuario(UUID id, String correo, String nombre, String rol, boolean puedeCrearCasos) { }

    DatosUsuario exigirDocente(UUID idUsuario);
    DatosUsuario registrarEstudiante(UUID idDocente, String correo, String nombre);
}
