package com.psicoapp.notifications.infrastructure;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CifradorNotificaciones {
    private final byte[] clave;
    public CifradorNotificaciones(@Value("${psicoapp.notificaciones.clave-cifrado:}") String configuracion) {
        try { clave = configuracion.isBlank() ? null : Base64.getDecoder().decode(configuracion); }
        catch (IllegalArgumentException excepcion) { throw new IllegalArgumentException("Clave de notificaciones Base64 invalida"); }
        if (clave != null && clave.length != 32) throw new IllegalArgumentException("La clave de notificaciones debe tener 32 bytes");
    }
    public String cifrar(String texto) {
        exigirClave();
        try {
            byte[] vector = new byte[12]; new SecureRandom().nextBytes(vector);
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, vector));
            byte[] datos = cifrador.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(vector) + "." + Base64.getEncoder().encodeToString(datos);
        } catch (Exception excepcion) { throw new IllegalStateException("No se pudo cifrar la notificacion"); }
    }
    public String descifrar(String texto) {
        exigirClave();
        try {
            String[] partes = texto.split("\\.", 2);
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.DECRYPT_MODE, new SecretKeySpec(clave, "AES"),
                    new GCMParameterSpec(128, Base64.getDecoder().decode(partes[0])));
            return new String(cifrador.doFinal(Base64.getDecoder().decode(partes[1])), StandardCharsets.UTF_8);
        } catch (Exception excepcion) { throw new IllegalStateException("No se pudo recuperar la notificacion cifrada"); }
    }
    private void exigirClave() {
        if (clave == null) throw new IllegalStateException("Configure PSICOAPP_NOTIFICACIONES_CLAVE_CIFRADO antes de convocar estudiantes");
    }
}
