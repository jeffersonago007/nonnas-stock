package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record UsuarioId(UUID value) implements EntityId<Usuario> {

    public UsuarioId {
        Objects.requireNonNull(value, "UsuarioId value must not be null");
    }

    public static UsuarioId of(UUID value) {
        return new UsuarioId(value);
    }

    public static UsuarioId generate() {
        return new UsuarioId(UUID.randomUUID());
    }
}
