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
    @Mock ConsultaUsoEspacios usoEspacios;
    private FachadaAdministracion academico;
    private final UUID idDocente = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        academico = new FachadaAdministracion(espacios, identidades, usoEspacios);
    }

    @Test
    @DisplayName("RF-04: bloquea antes de consultar uso y editar el catalogo")
    void editaEspacioBajoBloqueo() {
        UUID id = UUID.randomUUID();
        when(espacios.bloquearPorId(id)).thenReturn(Optional.of(new EspacioAcademico(id, "Anterior", "2026-2")));
        assertEquals("Nuevo", academico.editarEspacio(idDocente, id, " Nuevo ", "2027-1").nombre());
        var orden = inOrder(identidades, espacios, usoEspacios);
        orden.verify(identidades).exigirDocente(idDocente);
        orden.verify(espacios).bloquearPorId(id);
        orden.verify(usoEspacios).tieneSesiones(id);
        orden.verify(espacios).actualizar(new EspacioAcademico(id, "Nuevo", "2027-1"));
    }

    @Test
    @DisplayName("RF-04: no modifica un espacio usado ni inexistente")
    void rechazaEdicionDeEspacioUsadoOInexistente() {
        UUID id = UUID.randomUUID();
        when(espacios.bloquearPorId(id)).thenReturn(Optional.empty());
        assertThrows(NoEncontrado.class, () -> academico.editarEspacio(idDocente, id, "Nuevo", "2027-1"));
        verifyNoInteractions(usoEspacios);
        when(espacios.bloquearPorId(id)).thenReturn(Optional.of(new EspacioAcademico(id, "Anterior", "2026-2")));
        when(usoEspacios.tieneSesiones(id)).thenReturn(true);
        assertThrows(com.psicoapp.shared.exceptions.Conflicto.class,
                () -> academico.editarEspacio(idDocente, id, "Nuevo", "2027-1"));
        verify(espacios, never()).actualizar(any());
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
