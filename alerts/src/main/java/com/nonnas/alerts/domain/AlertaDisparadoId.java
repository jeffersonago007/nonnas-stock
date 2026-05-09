package com.nonnas.alerts.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record AlertaDisparadoId(UUID value) implements EntityId<AlertaDisparado> {
    public AlertaDisparadoId { Objects.requireNonNull(value); }
    public static AlertaDisparadoId of(UUID v) { return new AlertaDisparadoId(v); }
    public static AlertaDisparadoId generate() { return new AlertaDisparadoId(UUID.randomUUID()); }
}
