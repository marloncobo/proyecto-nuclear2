package com.psicoapp.participation.domain;

import com.psicoapp.sessions.domain.ModoNavegacion;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class Intento {
    private final UUID id;
    private final UUID idSesion;
    private final UUID idEstudiante;
    private final Instant inicio;
    private final Instant limite;
    private final ModoNavegacion modoNavegacion;
    private final List<UUID> preguntas;
    private final Map<UUID, UUID> respuestas;
    private final int posicionActual;
    private final EstadoIntento estado;

    private Intento(UUID id, UUID idSesion, UUID idEstudiante, Instant inicio, Instant limite,
            ModoNavegacion modoNavegacion, List<UUID> preguntas, Map<UUID, UUID> respuestas,
            int posicionActual, EstadoIntento estado) {
        this.id = exigir(id, "El intento es obligatorio");
        this.idSesion = exigir(idSesion, "La sesion es obligatoria");
        this.idEstudiante = exigir(idEstudiante, "El estudiante es obligatorio");
        this.inicio = Objects.requireNonNull(inicio, "El inicio es obligatorio");
        this.limite = Objects.requireNonNull(limite, "El limite es obligatorio");
        if (!inicio.isBefore(limite)) throw new IllegalArgumentException("El limite debe ser posterior al inicio");
        this.modoNavegacion = Objects.requireNonNull(modoNavegacion, "El modo es obligatorio");
        this.preguntas = validarPreguntas(preguntas);
        this.respuestas = Map.copyOf(respuestas);
        this.posicionActual = validarPosicion(posicionActual, this.preguntas.size());
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
    }

    public static Intento iniciar(UUID id, UUID idSesion, UUID idEstudiante, Instant inicio,
            int duracionMaximaMin, ModoNavegacion modoNavegacion, List<UUID> preguntas) {
        if (duracionMaximaMin <= 0) throw new IllegalArgumentException("La duracion debe ser positiva");
        Objects.requireNonNull(inicio, "El inicio es obligatorio");
        return new Intento(id, idSesion, idEstudiante, inicio,
                inicio.plus(Duration.ofMinutes(duracionMaximaMin)), modoNavegacion, preguntas,
                Map.of(), 0, EstadoIntento.EN_PROGRESO);
    }

    public static Intento desdePersistencia(UUID id, UUID idSesion, UUID idEstudiante, Instant inicio,
            Instant limite, ModoNavegacion modoNavegacion, List<UUID> preguntas,
            Map<UUID, UUID> respuestas, int posicionActual, EstadoIntento estado) {
        return new Intento(id, idSesion, idEstudiante, inicio, limite, modoNavegacion,
                preguntas, respuestas, posicionActual, estado);
    }

    public Intento responder(UUID idPregunta, UUID idOpcion, Instant ahora) {
        exigirActivo(ahora);
        exigirPreguntaActual(idPregunta);
        Objects.requireNonNull(idOpcion, "La opcion es obligatoria");
        Map<UUID, UUID> nuevasRespuestas = new LinkedHashMap<>(respuestas);
        nuevasRespuestas.put(idPregunta, idOpcion);
        return copiar(nuevasRespuestas, posicionActual, estado);
    }

    public Intento avanzar(Instant ahora) {
        exigirActivo(ahora);
        if (modoNavegacion == ModoNavegacion.RESTRINGIDA && !respuestas.containsKey(preguntaActual())) {
            throw new TransicionIntentoInvalida("Debe responder la pregunta antes de avanzar");
        }
        return posicionActual == preguntas.size() - 1
                ? this
                : copiar(respuestas, posicionActual + 1, estado);
    }

    public Intento retroceder(Instant ahora) {
        exigirActivo(ahora);
        if (modoNavegacion == ModoNavegacion.RESTRINGIDA) {
            throw new TransicionIntentoInvalida("La modalidad restringida no permite retroceder");
        }
        return posicionActual == 0 ? this : copiar(respuestas, posicionActual - 1, estado);
    }

    public Intento reanudar(Instant ahora) {
        exigirActivo(ahora);
        return this;
    }

    public Intento finalizar(Instant ahora) {
        exigirActivo(ahora);
        return copiar(respuestas, posicionActual, EstadoIntento.FINALIZADO);
    }

    public Intento vencer(Instant ahora) {
        exigirEstadoEnProgreso();
        Objects.requireNonNull(ahora, "El instante es obligatorio");
        if (!estaVencido(ahora)) throw new TransicionIntentoInvalida("El intento aun no ha vencido");
        return copiar(respuestas, posicionActual, EstadoIntento.VENCIDO);
    }

    public boolean estaVencido(Instant ahora) {
        Objects.requireNonNull(ahora, "El instante es obligatorio");
        return !ahora.isBefore(limite);
    }

    private void exigirActivo(Instant ahora) {
        Objects.requireNonNull(ahora, "El instante es obligatorio");
        exigirEstadoEnProgreso();
        if (estaVencido(ahora)) throw new TransicionIntentoInvalida("El tiempo del intento se agoto");
    }

    private void exigirEstadoEnProgreso() {
        if (estado != EstadoIntento.EN_PROGRESO) {
            throw new TransicionIntentoInvalida("El intento ya termino");
        }
    }

    private void exigirPreguntaActual(UUID idPregunta) {
        if (!preguntaActual().equals(idPregunta)) {
            throw new TransicionIntentoInvalida("La respuesta no corresponde a la pregunta actual");
        }
    }

    private UUID preguntaActual() {
        return preguntas.get(posicionActual);
    }

    private Intento copiar(Map<UUID, UUID> nuevasRespuestas, int nuevaPosicion, EstadoIntento nuevoEstado) {
        return new Intento(id, idSesion, idEstudiante, inicio, limite, modoNavegacion,
                preguntas, nuevasRespuestas, nuevaPosicion, nuevoEstado);
    }

    private static List<UUID> validarPreguntas(List<UUID> ids) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Debe existir al menos una pregunta");
        }
        List<UUID> copia = new ArrayList<>(ids);
        if (copia.stream().distinct().count() != copia.size()) {
            throw new IllegalArgumentException("Las preguntas no pueden repetirse");
        }
        return List.copyOf(copia);
    }

    private static int validarPosicion(int posicion, int cantidadPreguntas) {
        if (posicion < 0 || posicion >= cantidadPreguntas) throw new IllegalArgumentException("Posicion invalida");
        return posicion;
    }

    private static UUID exigir(UUID id, String mensaje) {
        return Objects.requireNonNull(id, mensaje);
    }

    public UUID id() { return id; }
    public UUID idSesion() { return idSesion; }
    public UUID idEstudiante() { return idEstudiante; }
    public Instant inicio() { return inicio; }
    public Instant limite() { return limite; }
    public ModoNavegacion modoNavegacion() { return modoNavegacion; }
    public List<UUID> preguntas() { return preguntas; }
    public Map<UUID, UUID> respuestas() { return respuestas; }
    public int posicionActual() { return posicionActual; }
    public UUID preguntaActualId() { return preguntaActual(); }
    public EstadoIntento estado() { return estado; }
}
