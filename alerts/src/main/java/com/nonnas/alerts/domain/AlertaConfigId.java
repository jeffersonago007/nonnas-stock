package com.nonnas.alerts.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record AlertaConfigId(UUID value) implements EntityId<AlertaConfig> {
    public AlertaConfigId { Objects.requireNonNull(value); }
    public static AlertaConfigId of(UUID v) { return new AlertaConfigId(v); }
    public static AlertaConfigId generate() { return new AlertaConfigId(UUID.randomUUID()); }
}
