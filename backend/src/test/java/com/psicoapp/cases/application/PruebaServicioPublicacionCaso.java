package com.psicoapp.cases.application;

import com.psicoapp.cases.DatosPruebaCasos;
import com.psicoapp.cases.domain.*;
import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.shared.exceptions.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PruebaServicioPublicacionCaso {

    @Test
    void archivaCasoPropioSinDuplicar() {
        Caso base = DatosPruebaCasos.completo(docente);
        when(casos.bloquearPorId(base.id())).thenReturn(Optional.of(base));
        when(casos.archivar(base.id(), docente)).thenReturn(true);
        assertEquals("ARCHIVADO", servicio.archivar(docente, base.id()).estado());
        var archivado = new Caso(base.id(), docente, base.nombre(), base.desarrolloSituacional(), base.contexto(),
                base.tiempoEstimadoMin(), base.escenarios(), base.rubrica(), EstadoCaso.ARCHIVADO);
        when(casos.bloquearPorId(base.id())).thenReturn(Optional.of(archivado));
        assertEquals("ARCHIVADO", servicio.archivar(docente, base.id()).estado());
        verify(casos, times(1)).archivar(base.id(), docente);
        assertThrows(AccesoDenegado.class, () -> servicio.archivar(UUID.randomUUID(), base.id()));
    }
    @Mock DirectorioIdentidad identidades;
    @Mock RepositorioCasos casos;
    private ServicioPublicacionCaso servicio;
    private final UUID docente = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        servicio = new ServicioPublicacionCaso(identidades, casos);
    }

    @Test
    @DisplayName("RF-09: autoriza, valida y publica el caso completo")
    void publicaCasoCompletoEnOrden() {
        Caso caso = DatosPruebaCasos.completo(docente);
        when(casos.bloquearPorId(caso.id())).thenReturn(Optional.of(caso));
        when(casos.publicar(caso.id(), docente)).thenReturn(true);
        var resultado = servicio.publicar(docente, caso.id());
        assertEquals("PUBLICADO", resultado.estado());
        assertEquals(caso.id(), resultado.id());
        var orden = inOrder(identidades, casos);
        orden.verify(identidades).exigirCreacionCasos(docente);
        orden.verify(casos).bloquearPorId(caso.id());
        orden.verify(casos).publicar(caso.id(), docente);
    }

    @Test
    @DisplayName("RF-02: permiso denegado no consulta ni modifica el caso")
    void rechazaSinPermiso() {
        doThrow(new AccesoDenegado("Sin permiso")).when(identidades).exigirCreacionCasos(docente);
        assertThrows(AccesoDenegado.class, () -> servicio.publicar(docente, UUID.randomUUID()));
        verifyNoInteractions(casos);
    }

    @Test
    @DisplayName("RF-02/09: propietario ajeno y caso inexistente no publican")
    void exigePropietarioYExistencia() {
        Caso ajeno = DatosPruebaCasos.completo(UUID.randomUUID());
        when(casos.bloquearPorId(ajeno.id())).thenReturn(Optional.of(ajeno));
        assertThrows(AccesoDenegado.class, () -> servicio.publicar(docente, ajeno.id()));
        assertThrows(NoEncontrado.class, () -> servicio.publicar(docente, UUID.randomUUID()));
        verify(casos, never()).publicar(any(), any());
    }

    @Test
    @DisplayName("RN-03/05: varios errores conservan borrador sin escritura parcial")
    void conservaBorradorInvalido() {
        Caso caso = new ConstructorCasoAcademico(UUID.randomUUID(), docente).construir();
        when(casos.bloquearPorId(caso.id())).thenReturn(Optional.of(caso));
        var error = assertThrows(PublicacionInvalida.class, () -> servicio.publicar(docente, caso.id()));
        assertEquals(6, error.resultado().errores().size());
        assertEquals("BORRADOR", caso.estado());
        verify(casos, never()).publicar(any(), any());
    }

    @Test
    @DisplayName("RF-09: repeticion y archivado no permiten nueva publicacion")
    void rechazaEstadosDistintosDeBorrador() {
        Caso base = DatosPruebaCasos.completo(docente);
        for (EstadoCaso estado : new EstadoCaso[]{EstadoCaso.PUBLICADO, EstadoCaso.ARCHIVADO}) {
            Caso caso = new Caso(base.id(), docente, base.nombre(), base.desarrolloSituacional(), base.contexto(),
                    base.tiempoEstimadoMin(), base.escenarios(), base.rubrica(), estado);
            when(casos.bloquearPorId(caso.id())).thenReturn(Optional.of(caso));
            assertThrows(Conflicto.class, () -> servicio.publicar(docente, caso.id()));
        }
        verify(casos, never()).publicar(any(), any());
    }

    @Test
    @DisplayName("RF-09: detecta perdida de la actualizacion condicional ante otra publicacion")
    void detectaCambioConcurrenteDeEstado() {
        Caso caso = DatosPruebaCasos.completo(docente);
        when(casos.bloquearPorId(caso.id())).thenReturn(Optional.of(caso));
        when(casos.publicar(caso.id(), docente)).thenReturn(false);
        assertThrows(Conflicto.class, () -> servicio.publicar(docente, caso.id()));
    }
}
