package com.psicoapp.cases.application;

import com.psicoapp.cases.domain.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PruebaServicioCreacionCaso {
    @Mock DirectorioIdentidad identidades;
    @Mock RepositorioCasos casos;
    private ServicioCreacionCaso servicio;
    private final UUID docente = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        servicio = new ServicioCreacionCaso(identidades, casos);
    }

    private ServicioCreacionCaso.DatosCreacionCaso datos(Integer tiempo) {
        return new ServicioCreacionCaso.DatosCreacionCaso("Caso", "Desarrollo", "Historia", tiempo, List.of(), null);
    }

    @Test
    @DisplayName("RF-02 / RF-05: autoriza antes de construir y guardar un borrador")
    void creaDespuesDeExigirPermiso() {
        var resultado = servicio.crear(docente, datos(40));
        var orden = inOrder(identidades, casos);
        orden.verify(identidades).exigirCreacionCasos(docente);
        orden.verify(casos).guardar(new Caso(resultado.id(), docente, "Caso", "Desarrollo", "Historia", 40, List.of(), null));
        assertEquals("BORRADOR", resultado.estado());
        assertNotEquals(resultado.id(), servicio.crear(docente, datos(40)).id());
    }

    @Test
    @DisplayName("RF-02: docente sin permiso no guarda casos")
    void rechazaSinPermisoAntesDePersistir() {
        doThrow(new AccesoDenegado("Sin permiso")).when(identidades).exigirCreacionCasos(docente);
        assertThrows(AccesoDenegado.class, () -> servicio.crear(docente, datos(40)));
        verifyNoInteractions(casos);
    }

    @Test
    @DisplayName("RF-05: datos invalidos no se persisten")
    void rechazaDatosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(docente, datos(-1)));
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(docente, null));
        verifyNoInteractions(casos);
    }

    @Test
    @DisplayName("RF-02 / CU-04: solo el responsable consulta el contenido y justificaciones del borrador")
    void protegeConsultaDelBorrador() {
        UUID id = UUID.randomUUID();
        Caso caso = new ConstructorCasoAcademico(id, docente).construir();
        when(casos.buscarPorId(id)).thenReturn(Optional.of(caso));
        assertEquals(id, servicio.consultar(docente, id).id());
        assertThrows(AccesoDenegado.class, () -> servicio.consultar(UUID.randomUUID(), id));
        assertThrows(NoEncontrado.class, () -> servicio.consultar(docente, UUID.randomUUID()));
    }
}
