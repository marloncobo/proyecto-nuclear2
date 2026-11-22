package com.psicoapp.shared.events;

import java.time.Instant;
import java.util.UUID;

public record SesionCerrada(UUID idSesion, Instant instante) { }
