package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record CargaInicialId(UUID value) implements EntityId<CargaInicial> {
    public CargaInicialId { Objects.requireNonNull(value); }
    public static CargaInicialId of(UUID v) { return new CargaInicialId(v); }
    public static CargaInicialId generate() { return new CargaInicialId(UUID.randomUUID()); }
}
