package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record UnidadeMedidaId(UUID value) implements EntityId<UnidadeMedida> {
    public UnidadeMedidaId {
        Objects.requireNonNull(value, "UnidadeMedidaId value must not be null");
    }
    public static UnidadeMedidaId of(UUID value) { return new UnidadeMedidaId(value); }
    public static UnidadeMedidaId generate() { return new UnidadeMedidaId(UUID.randomUUID()); }
}
