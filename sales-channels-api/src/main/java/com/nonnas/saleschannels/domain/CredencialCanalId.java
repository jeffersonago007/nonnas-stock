package com.nonnas.saleschannels.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record CredencialCanalId(UUID value) implements EntityId<CredencialCanal> {
    public CredencialCanalId { Objects.requireNonNull(value); }
    public static CredencialCanalId of(UUID v) { return new CredencialCanalId(v); }
    public static CredencialCanalId generate() { return new CredencialCanalId(UUID.randomUUID()); }
}
