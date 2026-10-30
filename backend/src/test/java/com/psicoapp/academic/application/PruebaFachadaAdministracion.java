package com.psicoapp.academic.application;

import com.psicoapp.academic.domain.EspacioAcademico;
import com.psicoapp.identity.application.DirectorioIdentidad;
import com.psicoapp.shared.exceptions.AccesoDenegado;
import com.psicoapp.shared.exceptions.NoEncontrado;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PruebaFachadaAdministracion {
    @Mock RepositorioEspaciosAcademicos espacios;
    @Mock DirectorioIdentidad identidades;
    private FachadaAdministracion academico;
    private final UUID idDocente = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        academico = new FachadaAdministracion(espacios, identidades);
    }

    @Test
    @DisplayName("RF-04 / ER ESPACIO_ACADEMICO: docente registra nombre y periodo")
    void creaEspacioDespuesDeAutorizarDocente() {
        var resultado = academico.crearEspacio(idDocente, " Psicologia social ", " 2026-2 ");
        assertEquals("Psicologia social", resultado.nombre());
        assertEquals("2026-2", resultado.periodo());
        var orden = inOrder(identidades, espacios);
        orden.verify(identidades).exigirDocente(idDocente);
        orden.verify(espacios).guardar(new EspacioAcademico(resultado.id(), resultado.nombre(), resultado.periodo()));
    }

    @Test
    @DisplayName("RF-02 / RF-04: sin rol docente no crea ni consulta espacios")
    void rechazaUsuarioSinPermisosAntesDeConsultarPersistencia() {
        doThrow(new AccesoDenegado("No autorizado")).when(identidades).exigirDocente(idDocente);
        assertThrows(AccesoDenegado.class, () -> academico.crearEspacio(idDocente, "Psicologia", "2026-2"));
        assertThrows(AccesoDenegado.class, () -> academico.listarEspacios(idDocente));
        assertThrows(AccesoDenegado.class, () -> academico.consultarEspacio(idDocente, UUID.randomUUID()));
        verifyNoInteractions(espacios);
    }

    @Test
    @DisplayName("RF-04: datos incompletos no se persisten")
    void rechazaEspacioAcademicoIncompleto() {
        assertThrows(IllegalArgumentException.class, () -> academico.crearEspacio(idDocente, " ", "2026-2"));
        assertThrows(IllegalArgumentException.class, () -> academico.crearEspacio(idDocente, "Psicologia", ""));
        verify(espacios, never()).guardar(any());
    }

    @Test
    @DisplayName("RF-04: consulta espacio existente por identificador")
    void consultaEspacioExistente() {
        EspacioAcademico espacio = new EspacioAcademico(UUID.randomUUID(), "Psicologia", "2026-2");
        when(espacios.buscarPorId(espacio.id())).thenReturn(Optional.of(espacio));
        assertEquals(new FachadaAdministracion.DatosEspacioAcademico(espacio.id(), espacio.nombre(), espacio.periodo()),
                academico.consultarEspacio(idDocente, espacio.id()));
    }

    @Test
    @DisplayName("RF-04: informa espacio inexistente")
    void informaEspacioInexistente() {
        assertThrows(NoEncontrado.class, () -> academico.consultarEspacio(idDocente, UUID.randomUUID()));
    }

    @Test
    @DisplayName("RF-04: lista espacios disponibles sin inventar membresias de estudiantes")
    void listaEspaciosAcademicos() {
        var lista = List.of(new EspacioAcademico(UUID.randomUUID(), "Psicologia", "2026-2"));
        when(espacios.listarTodos()).thenReturn(lista);
        assertEquals(List.of(new FachadaAdministracion.DatosEspacioAcademico(lista.getFirst().id(), "Psicologia", "2026-2")),
                academico.listarEspacios(idDocente));
    }
}
