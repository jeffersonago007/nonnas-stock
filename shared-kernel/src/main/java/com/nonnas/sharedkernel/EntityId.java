package com.nonnas.sharedkernel;

import java.util.UUID;

/**
 * Marker interface for typed entity identifiers across the project.
 *
 * <p>Each module declares its own concrete record (e.g.
 * {@code record InsumoId(UUID value) implements EntityId<Insumo>}) so that
 * the compiler treats {@code EntityId<Insumo>} and {@code EntityId<Filial>}
 * as distinct types in API signatures, preventing accidental ID swaps.
 *
 * <p>This interface is intentionally <em>not</em> sealed: Java sealing
 * requires permitted subtypes to live in the same package or named module,
 * which conflicts with our cross-Maven-module layout (and we do not use
 * JPMS). Cross-module type safety is provided by the generic phantom type
 * {@code T} alone, which is sufficient at compile time.
 *
 * @param <T> the entity type this identifier refers to (phantom type)
 */
public interface EntityId<T> {

    /**
     * The underlying UUID. Implementations must guarantee non-null.
     */
    UUID value();
}
