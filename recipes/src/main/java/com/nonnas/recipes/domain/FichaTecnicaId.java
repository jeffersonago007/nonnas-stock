package com.nonnas.recipes.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record FichaTecnicaId(UUID value) implements EntityId<FichaTecnica> {
    public FichaTecnicaId { Objects.requireNonNull(value); }
    public static FichaTecnicaId of(UUID v) { return new FichaTecnicaId(v); }
    public static FichaTecnicaId generate() { return new FichaTecnicaId(UUID.randomUUID()); }
}
