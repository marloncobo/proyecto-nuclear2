package com.psicoapp.notifications.infrastructure;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PruebaCifradorNotificaciones {
    private final CifradorNotificaciones cifrador = new CifradorNotificaciones("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
    @Test void usaVectoresDistintosYRecuperaUnicode() {
        String mensaje = "Convocatoria: codigo-ficticio, valoración y psicología";
        String primero = cifrador.cifrar(mensaje), segundo = cifrador.cifrar(mensaje);
        assertNotEquals(primero, segundo);
        assertFalse(primero.contains("codigo-ficticio"));
        assertEquals(mensaje, cifrador.descifrar(primero));
        assertEquals(mensaje, cifrador.descifrar(segundo));
    }
    @Test void rechazaContenidoManipuladoSinExponerOriginal() {
        String cifrado = cifrador.cifrar("codigo-ficticio");
        int separador = cifrado.indexOf('.') + 1;
        char reemplazo = cifrado.charAt(separador) == 'A' ? 'B' : 'A';
        String manipulado = cifrado.substring(0, separador) + reemplazo + cifrado.substring(separador + 1);
        var error = assertThrows(IllegalStateException.class, () -> cifrador.descifrar(manipulado));
        assertFalse(error.toString().contains("codigo-ficticio"));
        assertNull(error.getCause());
    }
    @Test void exigeClaveValidaYNoGeneraUnaNuevaAlReiniciar() {
        assertThrows(IllegalArgumentException.class, () -> new CifradorNotificaciones("no-es-base64"));
        assertThrows(IllegalArgumentException.class, () -> new CifradorNotificaciones("YQ=="));
        assertThrows(IllegalStateException.class, () -> new CifradorNotificaciones("").cifrar("codigo-ficticio"));
        assertThrows(IllegalStateException.class, () -> new CifradorNotificaciones("").descifrar("codigo-ficticio"));
    }
}
