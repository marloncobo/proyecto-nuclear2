package com.psicoapp.sessions.application;

import com.psicoapp.academic.application.FachadaAdministracion;
import com.psicoapp.cases.application.CatalogoCasos;
import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.sessions.DatosPruebaSesiones;
import com.psicoapp.sessions.domain.*;
import com.psicoapp.shared.exceptions.*;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static com.psicoapp.sessions.DatosPruebaSesiones.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class PruebaServicioGestionSesiones {

    @Test
    void requiereConvocadosValidosYSinDuplicados() {
        var base = datos(30);
        for (List<UUID> estudiantes : List.of(List.<UUID>of(), List.of(docente, docente))) {
            var invalido = new ServicioGestionSesiones.DatosProgramacion(base.idCaso(), base.idEspacio(), INICIO, FIN,
                    30, ModoNavegacion.FLEXIBLE, null, null, estudiantes);
            assertThrows(IllegalArgumentException.class, () -> servicio.programar(docente, invalido));
        }
        verifyNoInteractions(sesiones, convocatorias);
    }
    @Mock RepositorioSesiones sesiones;
    @Mock RepositorioConvocatorias convocatorias;
    @Mock DirectorioIdentidad identidades;
    @Mock CatalogoCasos casos;
    @Mock FachadaAdministracion academico;
    private ServicioGestionSesiones servicio;
    private final UUID docente = UUID.randomUUID();
    private final Sesion sesion = DatosPruebaSesiones.programada(docente);

    @BeforeEach
    void preparar() {
        servicio = new ServicioGestionSesiones(sesiones, identidades, casos, academico, Clock.fixed(INICIO, ZoneOffset.UTC), convocatorias);
    }

    private ServicioGestionSesiones.DatosProgramacion datos(int duracion) {
        return new ServicioGestionSesiones.DatosProgramacion(sesion.idCaso(), sesion.idEspacio(), INICIO, FIN,
                duracion, ModoNavegacion.FLEXIBLE, null, null, List.of(UUID.randomUUID()));
    }

    @Test
    @DisplayName("RF-04/18 / RN-03/13: programa tras validar identidad, caso publicado y espacio")
    void programaMedianteContratosPublicos() {
        doReturn(new CatalogoCasos.CasoProgramable(sesion.idCaso(), 40)).when(casos).exigirPublicado(docente, sesion.idCaso());
        var resultado = servicio.programar(docente, datos(30));
        assertEquals("SIN_INICIAR", resultado.estado());
        assertEquals(docente, resultado.idDocente());
        var orden = inOrder(identidades, casos, academico, sesiones);
        orden.verify(identidades).exigirDocente(docente);
        orden.verify(casos).exigirPublicado(docente, sesion.idCaso());
        orden.verify(academico).consultarEspacio(docente, sesion.idEspacio());
        orden.verify(sesiones).guardar(any());
    }

    @Test
    @DisplayName("RF-02: usuario sin rol docente no consulta dependencias ni persiste")
    void rechazaSinRolAntesDeAccederALosDatos() {
        doThrow(new AccesoDenegado("No docente")).when(identidades).exigirDocente(docente);
        assertThrows(AccesoDenegado.class, () -> servicio.programar(docente, datos(30)));
        assertThrows(AccesoDenegado.class, () -> servicio.iniciar(docente, sesion.id()));
        assertThrows(AccesoDenegado.class, () -> servicio.finalizar(docente, sesion.id()));
        assertThrows(AccesoDenegado.class, () -> servicio.consultar(docente, sesion.id()));
        verifyNoInteractions(sesiones, casos, academico);
    }

    @Test
    @DisplayName("RN-03/13: no programa caso no publicado, espacio inexistente ni duracion excesiva")
    void rechazaDependenciasYDuracionInvalidas() {
        when(casos.exigirPublicado(docente, sesion.idCaso())).thenThrow(new Conflicto("No publicado"));
        assertThrows(Conflicto.class, () -> servicio.programar(docente, datos(30)));
        verifyNoInteractions(academico, sesiones);
        doReturn(new CatalogoCasos.CasoProgramable(sesion.idCaso(), 40)).when(casos).exigirPublicado(docente, sesion.idCaso());
        when(academico.consultarEspacio(docente, sesion.idEspacio())).thenThrow(new NoEncontrado("Espacio"));
        assertThrows(NoEncontrado.class, () -> servicio.programar(docente, datos(30)));
        doReturn(null).when(academico).consultarEspacio(docente, sesion.idEspacio());
        assertThrows(IllegalArgumentException.class, () -> servicio.programar(docente, datos(41)));
        verify(sesiones, never()).guardar(any());
    }

    @Test
    @DisplayName("RF-19: inicia bajo bloqueo y no escribe al repetir cierre")
    void controlaTransicionesBajoBloqueo() {
        when(sesiones.bloquearPorId(sesion.id())).thenReturn(Optional.of(sesion));
        assertEquals("EN_CURSO", servicio.iniciar(docente, sesion.id()).estado());
        verify(sesiones).actualizarEstado(argThat(s -> s.estado().codigo() == TipoEstadoSesion.EN_CURSO));
        var cerrada = sesion.iniciar(INICIO).finalizar(MotivoCierre.DOCENTE, INICIO);
        when(sesiones.bloquearPorId(sesion.id())).thenReturn(Optional.of(cerrada));
        assertEquals("FINALIZADO", servicio.finalizar(docente, sesion.id()).estado());
        verify(sesiones, times(1)).actualizarEstado(any());
        assertThrows(Conflicto.class, () -> servicio.iniciar(docente, sesion.id()));
    }

    @Test
    @DisplayName("RF-02/19: solo el responsable administra y consulta; inexistente responde no encontrado")
    void protegePropiedadYExistencia() {
        when(sesiones.bloquearPorId(sesion.id())).thenReturn(Optional.of(sesion));
        when(sesiones.buscarPorId(sesion.id())).thenReturn(Optional.of(sesion));
        UUID otro = UUID.randomUUID();
        assertThrows(AccesoDenegado.class, () -> servicio.iniciar(otro, sesion.id()));
        assertThrows(AccesoDenegado.class, () -> servicio.finalizar(otro, sesion.id()));
        assertThrows(AccesoDenegado.class, () -> servicio.consultar(otro, sesion.id()));
        assertThrows(NoEncontrado.class, () -> servicio.iniciar(docente, UUID.randomUUID()));
        verify(sesiones, never()).actualizarEstado(any());
    }

    @Test
    @DisplayName("RN-12: cierre automatico revalida bajo bloqueo y omite ya cerradas")
    void cierraVencidasIncluyendoSesionNuncaIniciada() {
        var limite = FIN.plus(Duration.ofDays(1));
        var otra = DatosPruebaSesiones.programada(docente).iniciar(INICIO).finalizar(MotivoCierre.DOCENTE, INICIO);
        when(sesiones.buscarVencidas(FIN)).thenReturn(List.of(sesion.id(), otra.id()));
        when(sesiones.bloquearPorId(sesion.id())).thenReturn(Optional.of(sesion));
        when(sesiones.bloquearPorId(otra.id())).thenReturn(Optional.of(otra));
        assertEquals(1, servicio.cerrarVencidas(limite));
        verify(sesiones, times(1)).actualizarEstado(argThat(s -> s.id().equals(sesion.id())
                && s.estado().codigo() == TipoEstadoSesion.FINALIZADO));
    }

    @Test
    @DisplayName("RF-25 / RN-12: contrato de participacion bloquea y exige EN_CURSO sin exponer entidades")
    void consultaParticipacionBajoBloqueo() {
        when(sesiones.bloquearPorId(sesion.id())).thenReturn(Optional.of(sesion));
        assertThrows(Conflicto.class, () -> servicio.exigirEnCurso(sesion.id()));
        when(sesiones.bloquearPorId(sesion.id())).thenReturn(Optional.of(sesion.iniciar(INICIO)));
        var resultado = servicio.exigirEnCurso(sesion.id());
        assertEquals(sesion.idCaso(), resultado.idCaso());
        assertEquals("FLEXIBLE", resultado.modoNavegacion());
        verify(sesiones, never()).actualizarEstado(any());
    }
}
