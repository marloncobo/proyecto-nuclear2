package com.psicoapp.identity.application;

import com.psicoapp.identity.domain.Rol;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PruebaFiltroUsuarios {
    @Test
    void rn10NormalizaBusquedaSinAlterarRolOEstado() {
        var filtro = new FiltroUsuarios(Rol.DOCENTE, false, "  NOMBRE+GRUPO@EXAMPLE.TEST  ");
        assertEquals("nombre+grupo@example.test", filtro.busqueda());
        assertEquals(Rol.DOCENTE, filtro.rol());
        assertEquals(false, filtro.activo());
    }

    @Test
    void rf03AceptaBusquedaOpcionalYLimitaLongitud() {
        assertEquals("", new FiltroUsuarios(null, null, null).busqueda());
        assertEquals("", new FiltroUsuarios(null, null, "  ").busqueda());
        assertEquals(180, new FiltroUsuarios(null, null, "a".repeat(180)).busqueda().length());
        assertThrows(IllegalArgumentException.class, () -> new FiltroUsuarios(null, null, "a".repeat(181)));
    }
}
