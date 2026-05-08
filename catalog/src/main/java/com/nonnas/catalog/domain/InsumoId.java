package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record InsumoId(UUID value) implements EntityId<Insumo> {
    public InsumoId {
        Objects.requireNonNull(value, "InsumoId value must not be null");
    }
    public static InsumoId of(UUID value) { return new InsumoId(value); }
    public static InsumoId generate() { return new InsumoId(UUID.randomUUID()); }
}
