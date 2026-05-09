package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record TransferenciaId(UUID value) implements EntityId<Transferencia> {
    public TransferenciaId { Objects.requireNonNull(value); }
    public static TransferenciaId of(UUID v) { return new TransferenciaId(v); }
    public static TransferenciaId generate() { return new TransferenciaId(UUID.randomUUID()); }
}
