package com.nonnas.saleschannels.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record EventoCanalId(UUID value) implements EntityId<EventoCanal> {
    public EventoCanalId { Objects.requireNonNull(value); }
    public static EventoCanalId of(UUID v) { return new EventoCanalId(v); }
    public static EventoCanalId generate() { return new EventoCanalId(UUID.randomUUID()); }
}
