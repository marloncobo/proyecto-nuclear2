package com.psicoapp.cases.application;

import com.psicoapp.cases.DatosPruebaCasos;
import com.psicoapp.cases.domain.*;
import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.shared.exceptions.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PruebaServicioConsultaCasos {
    private final RepositorioCasos casos = mock(RepositorioCasos.class);
    private final DirectorioIdentidad identidades = mock(DirectorioIdentidad.class);
    private final ServicioConsultaCasos servicio = new ServicioConsultaCasos(casos, identidades);
    private final UUID docente = UUID.randomUUID();
    private final Caso base = DatosPruebaCasos.completo(docente);

    private Caso conEstado(EstadoCaso estado) {
        return new Caso(base.id(), docente, base.nombre(), base.desarrolloSituacional(), base.contexto(),
                base.tiempoEstimadoMin(), base.escenarios(), base.rubrica(), estado);
    }

    @Test
    @DisplayName("RF-15: seguimiento admite casos propios archivados y exige rol, propiedad y existencia")
    void autorizaSeguimientoSinExigirPublicacion() {
        when(casos.buscarPorId(base.id())).thenReturn(Optional.of(conEstado(EstadoCaso.ARCHIVADO)));
        assertDoesNotThrow(() -> servicio.exigirResponsable(docente, base.id()));
        assertThrows(AccesoDenegado.class, () -> servicio.exigirResponsable(UUID.randomUUID(), base.id()));
        assertThrows(NoEncontrado.class, () -> servicio.exigirResponsable(docente, UUID.randomUUID()));
        doThrow(new AccesoDenegado("No docente")).when(identidades).exigirDocente(docente);
        assertThrows(AccesoDenegado.class, () -> servicio.exigirResponsable(docente, base.id()));
    }

    @Test
    @DisplayName("RN-03 / RF-18: contrato de programacion expone solo id y tiempo de caso publicado propio")
    void permiteCasoPublicadoDelDocente() {
        when(casos.bloquearPorId(base.id())).thenReturn(Optional.of(conEstado(EstadoCaso.PUBLICADO)));
        assertEquals(new CatalogoCasos.CasoProgramable(base.id(), 40, base.nombre()), servicio.exigirPublicado(docente, base.id()));
    }

    @Test
    @DisplayName("RN-03 / RF-09: no programa borradores ni archivados")
    void rechazaEstadoNoProgramable() {
        for (EstadoCaso estado : new EstadoCaso[]{EstadoCaso.BORRADOR, EstadoCaso.ARCHIVADO}) {
            when(casos.bloquearPorId(base.id())).thenReturn(Optional.of(conEstado(estado)));
            assertThrows(Conflicto.class, () -> servicio.exigirPublicado(docente, base.id()));
        }
    }

    @Test
    @DisplayName("RF-02: exige identidad, propietario y existencia")
    void protegeContratoEntreModulos() {
        when(casos.bloquearPorId(base.id())).thenReturn(Optional.of(conEstado(EstadoCaso.PUBLICADO)));
        assertThrows(AccesoDenegado.class, () -> servicio.exigirPublicado(UUID.randomUUID(), base.id()));
        assertThrows(NoEncontrado.class, () -> servicio.exigirPublicado(docente, UUID.randomUUID()));
        doThrow(new AccesoDenegado("No docente")).when(identidades).exigirDocente(docente);
        assertThrows(AccesoDenegado.class, () -> servicio.exigirPublicado(docente, base.id()));
    }
}
