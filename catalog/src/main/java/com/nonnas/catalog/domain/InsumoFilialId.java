package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record InsumoFilialId(UUID value) implements EntityId<InsumoFilial> {
    public InsumoFilialId {
        Objects.requireNonNull(value, "InsumoFilialId value must not be null");
    }
    public static InsumoFilialId of(UUID value) { return new InsumoFilialId(value); }
    public static InsumoFilialId generate() { return new InsumoFilialId(UUID.randomUUID()); }
}
