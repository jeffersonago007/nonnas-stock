package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record FornecedorId(UUID value) implements EntityId<Fornecedor> {
    public FornecedorId {
        Objects.requireNonNull(value, "FornecedorId value must not be null");
    }
    public static FornecedorId of(UUID value) { return new FornecedorId(value); }
    public static FornecedorId generate() { return new FornecedorId(UUID.randomUUID()); }
}
