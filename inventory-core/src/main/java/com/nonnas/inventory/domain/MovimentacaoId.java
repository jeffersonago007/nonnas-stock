package com.nonnas.inventory.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record MovimentacaoId(UUID value) implements EntityId<Movimentacao> {
    public MovimentacaoId { Objects.requireNonNull(value); }
    public static MovimentacaoId of(UUID v) { return new MovimentacaoId(v); }
    public static MovimentacaoId generate() { return new MovimentacaoId(UUID.randomUUID()); }
}
