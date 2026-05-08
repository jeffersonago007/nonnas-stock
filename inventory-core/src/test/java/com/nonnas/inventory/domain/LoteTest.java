package com.nonnas.inventory.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoteTest {

    private static final Instant T0 = Instant.parse("2026-05-08T12:00:00Z");

    @Test
    void novoLoteValido() {
        Lote l = Lote.novo(UUID.randomUUID(), null, null, "L-001",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 12, 1),
                new BigDecimal("25.0000"), T0);
        assertThat(l.numeroLote()).isEqualTo("L-001");
        assertThat(l.fornecedorIdOpt()).isEmpty();
        assertThat(l.dataValidadeOpt()).contains(LocalDate.of(2026, 12, 1));
    }

    @Test
    void rejeitaNumeroLoteVazio() {
        assertThatThrownBy(() -> Lote.novo(UUID.randomUUID(), null, null, "  ",
                null, null, new BigDecimal("1"), T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaValorNegativo() {
        assertThatThrownBy(() -> Lote.novo(UUID.randomUUID(), null, null, "L-001",
                null, null, new BigDecimal("-1"), T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaValidadeAntesDaFabricacao() {
        assertThatThrownBy(() -> Lote.novo(UUID.randomUUID(), null, null, "L-001",
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 5, 1),
                BigDecimal.ONE, T0))
                .isInstanceOf(ValidationException.class);
    }
}
