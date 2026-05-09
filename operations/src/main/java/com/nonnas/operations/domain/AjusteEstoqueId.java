package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record AjusteEstoqueId(UUID value) implements EntityId<AjusteEstoque> {
    public AjusteEstoqueId { Objects.requireNonNull(value); }
    public static AjusteEstoqueId of(UUID v) { return new AjusteEstoqueId(v); }
    public static AjusteEstoqueId generate() { return new AjusteEstoqueId(UUID.randomUUID()); }
}
