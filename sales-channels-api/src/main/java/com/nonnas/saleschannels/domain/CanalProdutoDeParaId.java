package com.nonnas.saleschannels.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record CanalProdutoDeParaId(UUID value) implements EntityId<CanalProdutoDePara> {
    public CanalProdutoDeParaId { Objects.requireNonNull(value); }
    public static CanalProdutoDeParaId of(UUID v) { return new CanalProdutoDeParaId(v); }
    public static CanalProdutoDeParaId generate() { return new CanalProdutoDeParaId(UUID.randomUUID()); }
}
