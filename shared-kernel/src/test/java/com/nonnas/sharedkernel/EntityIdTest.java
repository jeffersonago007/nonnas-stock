package com.nonnas.sharedkernel;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityIdTest {

    /** Sample marker used only to instantiate a concrete EntityId in this test. */
    private static final class Insumo {}
    private static final class Filial {}

    private record InsumoId(UUID value) implements EntityId<Insumo> {}
    private record FilialId(UUID value) implements EntityId<Filial> {}

    @Test
    void typedIdsHoldTheirUuid() {
        UUID raw = UUID.randomUUID();
        InsumoId id = new InsumoId(raw);
        assertThat(id.value()).isEqualTo(raw);
    }

    @Test
    void differentEntityIdRecordsAreNotEqualEvenWithSameUuid() {
        UUID raw = UUID.randomUUID();
        InsumoId i = new InsumoId(raw);
        FilialId f = new FilialId(raw);
        // record equality is type-aware: different record types are never equal,
        // even when their components match. This is the runtime guarantee that
        // complements the compile-time generic phantom-type protection.
        assertThat((Object) i).isNotEqualTo(f);
    }
}
