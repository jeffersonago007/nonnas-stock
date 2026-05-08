package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record CategoriaInsumoId(UUID value) implements EntityId<CategoriaInsumo> {
    public CategoriaInsumoId {
        Objects.requireNonNull(value, "CategoriaInsumoId value must not be null");
    }
    public static CategoriaInsumoId of(UUID value) { return new CategoriaInsumoId(value); }
    public static CategoriaInsumoId generate() { return new CategoriaInsumoId(UUID.randomUUID()); }
}
