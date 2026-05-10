package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.EntityId;

import java.util.Objects;
import java.util.UUID;

public record NotaFiscalId(UUID value) implements EntityId<NotaFiscal> {
    public NotaFiscalId { Objects.requireNonNull(value); }
    public static NotaFiscalId of(UUID v) { return new NotaFiscalId(v); }
    public static NotaFiscalId generate() { return new NotaFiscalId(UUID.randomUUID()); }
}
