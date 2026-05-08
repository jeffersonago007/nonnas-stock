package com.nonnas.sharedkernel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable quantity value bound to a unit of measure.
 *
 * <p>The unit is referenced only by its UUID — the catalog module owns the
 * full {@code UnidadeMedida} aggregate and its conversion rules
 * ({@code ConversorUnidadeService} in T03). Arithmetic on quantities of
 * different units is rejected at this level: callers must convert via the
 * catalog service before operating.
 *
 * <p>Scale is fixed at 4 decimal places ({@link RoundingMode#HALF_EVEN}),
 * sufficient to express e.g. "0.0050 KG" = 5g without loss.
 */
public record Quantity(BigDecimal value, UUID unidadeMedidaId) {

    public static final int SCALE = 4;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public Quantity {
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(unidadeMedidaId, "unidadeMedidaId must not be null");
        value = value.setScale(SCALE, ROUNDING);
    }

    public static Quantity of(BigDecimal value, UUID unidadeMedidaId) {
        return new Quantity(value, unidadeMedidaId);
    }

    public static Quantity of(String value, UUID unidadeMedidaId) {
        return new Quantity(new BigDecimal(value), unidadeMedidaId);
    }

    public static Quantity zero(UUID unidadeMedidaId) {
        return new Quantity(BigDecimal.ZERO, unidadeMedidaId);
    }

    public Quantity add(Quantity other) {
        requireSameUnit(other);
        return new Quantity(value.add(other.value), unidadeMedidaId);
    }

    public Quantity subtract(Quantity other) {
        requireSameUnit(other);
        return new Quantity(value.subtract(other.value), unidadeMedidaId);
    }

    public Quantity multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must not be null");
        return new Quantity(value.multiply(factor), unidadeMedidaId);
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    public boolean isNegative() {
        return value.signum() < 0;
    }

    public boolean isZero() {
        return value.signum() == 0;
    }

    private void requireSameUnit(Quantity other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!unidadeMedidaId.equals(other.unidadeMedidaId)) {
            throw new IllegalArgumentException(
                    "Cannot operate on different units: " + unidadeMedidaId + " vs " + other.unidadeMedidaId);
        }
    }
}
