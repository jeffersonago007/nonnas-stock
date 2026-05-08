package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record FilialId(UUID value) implements EntityId<Filial> {

    public FilialId {
        Objects.requireNonNull(value, "FilialId value must not be null");
    }

    public static FilialId of(UUID value) {
        return new FilialId(value);
    }

    public static FilialId generate() {
        return new FilialId(UUID.randomUUID());
    }
}
