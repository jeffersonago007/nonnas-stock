package com.nonnas.sharedkernel;

import com.nonnas.sharedkernel.testsupport.QuantityFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @Test
    @DisplayName("Quantity.of normaliza para escala 4")
    void scaleNormalized() {
        Quantity q = Quantity.of(new BigDecimal("1.5"), QuantityFixtures.KG);
        assertThat(q.value()).isEqualByComparingTo("1.5000");
        assertThat(q.value().scale()).isEqualTo(Quantity.SCALE);
    }

    @Test
    @DisplayName("Quantity.of(String, UUID) parse e normaliza")
    void fromString() {
        Quantity q = Quantity.of("0.25", QuantityFixtures.KG);
        assertThat(q.value()).isEqualByComparingTo("0.25");
    }

    @Test
    @DisplayName("Quantity.zero produz quantidade zero na unidade")
    void zero() {
        Quantity q = Quantity.zero(QuantityFixtures.KG);
        assertThat(q.isZero()).isTrue();
        assertThat(q.unidadeMedidaId()).isEqualTo(QuantityFixtures.KG);
    }

    @Test
    @DisplayName("value nulo lança NullPointerException")
    void valueNullRejected() {
        assertThatThrownBy(() -> new Quantity(null, QuantityFixtures.KG))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("unidadeMedidaId nulo lança NullPointerException")
    void unitNullRejected() {
        assertThatThrownBy(() -> new Quantity(BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("add soma quantidades da mesma unidade")
    void addSameUnit() {
        Quantity total = QuantityFixtures.kg("1.5").add(QuantityFixtures.kg("0.5"));
        assertThat(total.value()).isEqualByComparingTo("2.0000");
    }

    @Test
    @DisplayName("subtract subtrai na mesma unidade")
    void subtractSameUnit() {
        Quantity remaining = QuantityFixtures.kg("2").subtract(QuantityFixtures.kg("0.5"));
        assertThat(remaining.value()).isEqualByComparingTo("1.5000");
    }

    @Test
    @DisplayName("multiply aplica fator e mantém escala")
    void multiplyAppliesFactor() {
        Quantity tripled = QuantityFixtures.kg("0.5").multiply(new BigDecimal("3"));
        assertThat(tripled.value()).isEqualByComparingTo("1.5000");
    }

    @Test
    @DisplayName("multiply com fator nulo lança NullPointerException")
    void multiplyNullFactor() {
        assertThatThrownBy(() -> QuantityFixtures.oneKg().multiply(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("operação entre unidades diferentes lança IllegalArgumentException")
    void mixedUnitsRejected() {
        assertThatThrownBy(() -> QuantityFixtures.kg("1").add(QuantityFixtures.grams("100")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different units");
    }

    @Test
    @DisplayName("subtract entre unidades diferentes lança IllegalArgumentException")
    void subtractMixedUnitsRejected() {
        assertThatThrownBy(() -> QuantityFixtures.kg("1").subtract(QuantityFixtures.grams("100")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("add com null lança NullPointerException")
    void addNullRejected() {
        assertThatThrownBy(() -> QuantityFixtures.oneKg().add(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("predicados de sinal")
    void signPredicates() {
        assertThat(QuantityFixtures.oneKg().isPositive()).isTrue();
        assertThat(QuantityFixtures.oneKg().isNegative()).isFalse();
        assertThat(QuantityFixtures.oneKg().isZero()).isFalse();
        assertThat(Quantity.zero(QuantityFixtures.KG).isZero()).isTrue();
        assertThat(QuantityFixtures.kg("-1").isNegative()).isTrue();
    }

    @Test
    @DisplayName("equality compara valor e unidade")
    void equality() {
        Quantity a = Quantity.of("1.0000", QuantityFixtures.KG);
        Quantity b = Quantity.of("1.0000", QuantityFixtures.KG);
        Quantity c = Quantity.of("1.0000", UUID.randomUUID());
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }
}
