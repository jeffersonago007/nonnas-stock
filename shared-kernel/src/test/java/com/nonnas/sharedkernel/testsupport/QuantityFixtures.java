package com.nonnas.sharedkernel.testsupport;

import com.nonnas.sharedkernel.Quantity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Test fixtures for {@link Quantity}. The {@code KG} and {@code G} UUIDs
 * are stable across the test run so different tests in the suite agree on
 * what "the kg unit" is.
 */
public final class QuantityFixtures {

    public static final UUID KG = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID G = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private QuantityFixtures() {
    }

    public static Quantity oneKg() {
        return Quantity.of(BigDecimal.ONE, KG);
    }

    public static Quantity kg(String amount) {
        return Quantity.of(amount, KG);
    }

    public static Quantity grams(String amount) {
        return Quantity.of(amount, G);
    }
}
