package com.nonnas.sharedkernel.testsupport;

import com.nonnas.sharedkernel.Money;

import java.math.BigDecimal;

/**
 * Test fixtures for {@link Money}. Reusable across modules via test-jar
 * once that dependency is enabled (currently consumed only by shared-kernel
 * itself).
 */
public final class MoneyFixtures {

    private MoneyFixtures() {
    }

    public static Money tenReais() {
        return Money.brl("10.00");
    }

    public static Money oneCent() {
        return Money.brl("0.01");
    }

    public static Money brl(String amount) {
        return Money.brl(new BigDecimal(amount));
    }
}
