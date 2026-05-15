package com.nonnas.saleschannels.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record PedidoCanalId(UUID value) implements EntityId<PedidoCanal> {
    public PedidoCanalId { Objects.requireNonNull(value); }
    public static PedidoCanalId of(UUID v) { return new PedidoCanalId(v); }
    public static PedidoCanalId generate() { return new PedidoCanalId(UUID.randomUUID()); }
}
