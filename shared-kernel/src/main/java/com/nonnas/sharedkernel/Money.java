package com.nonnas.sharedkernel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable monetary value. Always carries an explicit {@link Currency}
 * to prevent silent currency mixing, and uses {@link RoundingMode#HALF_EVEN}
 * (banker's rounding) for arithmetic results.
 *
 * <p>Two scales are used:
 * <ul>
 *   <li>{@code UNIT_SCALE} (4) for unit prices and intermediate results,
 *       sufficient to keep cents-of-cents precision in chained operations.
 *       Factory methods and arithmetic operations normalize to this scale.</li>
 *   <li>{@code TOTAL_SCALE} (2) for final, displayable amounts. Use
 *       {@link #asTotal()} to round to this scale before persisting or
 *       returning to the client.</li>
 * </ul>
 *
 * <p>The canonical constructor itself does <em>not</em> normalize scale: it
 * trusts the caller. This is what allows {@link #asTotal()} to produce a
 * 2-decimal {@code Money} without the constructor immediately bumping it
 * back to 4. Always go through the factory methods or arithmetic methods
 * unless you specifically need to preserve a non-standard scale.
 *
 * <p>Arithmetic on different currencies throws
 * {@link IllegalArgumentException}; this is a programmer error, not a
 * domain exception.
 */
public record Money(BigDecimal amount, Currency currency) {

    public static final Currency BRL = Currency.getInstance("BRL");
    public static final int UNIT_SCALE = 4;
    public static final int TOTAL_SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
    }

    public static Money brl(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        return new Money(amount.setScale(UNIT_SCALE, ROUNDING), BRL);
    }

    public static Money brl(String amount) {
        return brl(new BigDecimal(amount));
    }

    public static Money brl(long amount) {
        return brl(BigDecimal.valueOf(amount));
    }

    public static Money zero(Currency currency) {
        Objects.requireNonNull(currency, "currency must not be null");
        return new Money(BigDecimal.ZERO.setScale(UNIT_SCALE, ROUNDING), currency);
    }

    public static Money zeroBrl() {
        return zero(BRL);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount).setScale(UNIT_SCALE, ROUNDING), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount).setScale(UNIT_SCALE, ROUNDING), currency);
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must not be null");
        return new Money(amount.multiply(factor).setScale(UNIT_SCALE, ROUNDING), currency);
    }

    public Money divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "divisor must not be null");
        if (divisor.signum() == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return new Money(amount.divide(divisor, UNIT_SCALE, ROUNDING), currency);
    }

    /**
     * Returns a new Money rounded to the display scale (2 decimal places).
     * Use this just before persisting or returning to the client. Subsequent
     * arithmetic via {@link #add(Money)} etc. promotes the result back to
     * {@link #UNIT_SCALE}.
     */
    public Money asTotal() {
        return new Money(amount.setScale(TOTAL_SCALE, ROUNDING), currency);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot operate on different currencies: " + currency + " and " + other.currency);
        }
    }
}
