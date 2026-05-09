package com.nonnas.recipes.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record ProdutoVendavelId(UUID value) implements EntityId<ProdutoVendavel> {
    public ProdutoVendavelId { Objects.requireNonNull(value); }
    public static ProdutoVendavelId of(UUID v) { return new ProdutoVendavelId(v); }
    public static ProdutoVendavelId generate() { return new ProdutoVendavelId(UUID.randomUUID()); }
}
