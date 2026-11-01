package com.psicoapp.cases.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** Invariantes estructurales del borrador; no sustituye la validacion de publicacion. */
final class ContenidoCaso {
    private ContenidoCaso() { }

    static String texto(String valor, int maximo) {
        String texto = valor == null ? "" : valor.strip();
        if (texto.length() > maximo) {
            throw new IllegalArgumentException("El texto supera la longitud permitida");
        }
        return texto;
    }

    static void identificador(UUID id) {
        if (id == null) throw new IllegalArgumentException("El identificador es obligatorio");
    }

    static <T> List<T> copiar(List<T> elementos) {
        if (elementos == null) throw new IllegalArgumentException("La coleccion es obligatoria");
        if (elementos.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("La coleccion no admite elementos nulos");
        }
        return List.copyOf(elementos);
    }

    static void unicos(List<?> valores) {
        if (new HashSet<>(valores).size() != valores.size()) {
            throw new IllegalArgumentException("No se admiten identificadores ni posiciones duplicados");
        }
    }

    static void orden(int orden) {
        if (orden < 1) throw new IllegalArgumentException("El orden debe ser positivo");
    }

    static void decimal(BigDecimal valor, int enteros, int decimales, boolean admiteCero) {
        if (valor == null || valor.signum() < 0 || (!admiteCero && valor.signum() == 0)
                || valor.stripTrailingZeros().scale() > decimales
                || valor.compareTo(BigDecimal.TEN.pow(enteros)) >= 0) {
            throw new IllegalArgumentException("Valor decimal fuera de rango");
        }
    }
}
