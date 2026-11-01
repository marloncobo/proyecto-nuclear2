package com.psicoapp.sessions.application;

import com.psicoapp.sessions.DatosPruebaSesiones;
import com.psicoapp.sessions.domain.*;
import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PruebaVerificadorConvocatorias {
    @Test
    void codigoSeAsociaAEstudianteYSesionYRespetaMitadDelHorario() {
        var convocatorias = mock(RepositorioConvocatorias.class);
        var sesiones = mock(RepositorioSesiones.class);
        var identidades = mock(DirectorioIdentidad.class);
        var reloj = mock(Clock.class);
        var sesion = DatosPruebaSesiones.programada(UUID.randomUUID()).iniciar(DatosPruebaSesiones.INICIO);
        UUID estudiante = UUID.randomUUID();
        String codigo = UUID.randomUUID().toString().replace("-", "");
        var convocatoria = new Convocatoria(UUID.randomUUID(), sesion.id(), estudiante, Convocatoria.resumir(codigo), true);
        when(convocatorias.buscarAutorizada(estudiante, Convocatoria.resumir(codigo))).thenReturn(Optional.of(convocatoria));
        when(sesiones.buscarPorId(sesion.id())).thenReturn(Optional.of(sesion));
        when(sesiones.bloquearPorId(sesion.id())).thenReturn(Optional.of(sesion));
        var verificador = new VerificadorConvocatorias(convocatorias, sesiones, reloj, identidades);
        when(reloj.instant()).thenReturn(DatosPruebaSesiones.INICIO.plusSeconds(1800));
        assertTrue(verificador.verificar(estudiante, codigo));
        assertFalse(verificador.verificar(UUID.randomUUID(), codigo));
        assertFalse(verificador.verificar(estudiante, "invalido"));
        assertDoesNotThrow(() -> verificador.exigirAcceso(estudiante, sesion.id(), codigo));
        when(reloj.instant()).thenReturn(DatosPruebaSesiones.INICIO.plusSeconds(1801));
        assertThrows(AccesoDenegado.class, () -> verificador.exigirAcceso(estudiante, sesion.id(), codigo));
        // Autenticacion no es ingreso nuevo: no bloquea por si sola reanudaciones despues del umbral.
        assertTrue(verificador.verificar(estudiante, codigo));
        when(reloj.instant()).thenReturn(DatosPruebaSesiones.FIN.plus(Duration.ofDays(1)));
        assertFalse(verificador.verificar(estudiante, codigo));
    }

    @Test
    void codigoDeOtraConvocatoriaNoAutorizaLaSesionSolicitada() {
        var convocatorias = mock(RepositorioConvocatorias.class);
        var sesiones = mock(RepositorioSesiones.class);
        var identidades = mock(DirectorioIdentidad.class);
        var sesion = DatosPruebaSesiones.programada(UUID.randomUUID()).iniciar(DatosPruebaSesiones.INICIO);
        UUID estudiante = UUID.randomUUID();
        String codigo = UUID.randomUUID().toString().replace("-", "");
        when(sesiones.bloquearPorId(sesion.id())).thenReturn(Optional.of(sesion));
        when(convocatorias.buscarAutorizada(estudiante, Convocatoria.resumir(codigo)))
                .thenReturn(Optional.of(new Convocatoria(UUID.randomUUID(), UUID.randomUUID(), estudiante, Convocatoria.resumir(codigo), true)));
        var verificador = new VerificadorConvocatorias(convocatorias, sesiones,
                Clock.fixed(DatosPruebaSesiones.INICIO, ZoneOffset.UTC), identidades);
        assertThrows(AccesoDenegado.class, () -> verificador.exigirAcceso(estudiante, sesion.id(), codigo));
    }
}
