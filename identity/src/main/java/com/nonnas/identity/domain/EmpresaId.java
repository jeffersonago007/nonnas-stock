package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record EmpresaId(UUID value) implements EntityId<Empresa> {

    public EmpresaId {
        Objects.requireNonNull(value, "EmpresaId value must not be null");
    }

    public static EmpresaId of(UUID value) {
        return new EmpresaId(value);
    }

    public static EmpresaId generate() {
        return new EmpresaId(UUID.randomUUID());
    }
}
