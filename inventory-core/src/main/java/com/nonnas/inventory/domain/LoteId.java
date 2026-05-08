package com.nonnas.inventory.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record LoteId(UUID value) implements EntityId<Lote> {
    public LoteId { Objects.requireNonNull(value); }
    public static LoteId of(UUID v) { return new LoteId(v); }
    public static LoteId generate() { return new LoteId(UUID.randomUUID()); }
}
