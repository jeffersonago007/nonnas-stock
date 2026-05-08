package com.nonnas.inventory.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SaldoLoteTest {

    @Test
    void zeroAcrescentarESubtrair() {
        Instant t = Instant.parse("2026-05-08T12:00:00Z");
        SaldoLote s = SaldoLote.zero(LoteId.generate(), UUID.randomUUID(), t);
        assertThat(s.quantidadeBase()).isEqualByComparingTo("0");
        SaldoLote depois = s.acrescentar(new BigDecimal("10"), t).subtrair(new BigDecimal("3"), t);
        assertThat(depois.quantidadeBase()).isEqualByComparingTo("7");
        assertThat(depois.isPositivo()).isTrue();
    }

    @Test
    void permiteNegativo() {
        Instant t = Instant.parse("2026-05-08T12:00:00Z");
        SaldoLote s = SaldoLote.zero(LoteId.generate(), UUID.randomUUID(), t);
        SaldoLote neg = s.subtrair(new BigDecimal("5"), t);
        assertThat(neg.isNegativo()).isTrue();
    }
}
