package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsumoFilialTest {

    private static final Instant T0 = Instant.parse("2026-05-08T12:00:00Z");

    @Test
    void novoComParametrosBasicos() {
        InsumoFilial f = InsumoFilial.novo(InsumoId.generate(), UUID.randomUUID(),
                new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("20"), T0);
        assertThat(f.estoqueMinimo()).isEqualByComparingTo("10");
        assertThat(f.estoqueMaximo()).contains(new BigDecimal("100"));
        assertThat(f.pontoPedido()).contains(new BigDecimal("20"));
        assertThat(f.ativo()).isTrue();
    }

    @Test
    void minimoNuloViraZero() {
        InsumoFilial f = InsumoFilial.novo(InsumoId.generate(), UUID.randomUUID(),
                null, null, null, T0);
        assertThat(f.estoqueMinimo()).isEqualByComparingTo("0");
        assertThat(f.estoqueMaximo()).isEmpty();
        assertThat(f.pontoPedido()).isEmpty();
    }

    @Test
    void atualizarParametros() {
        InsumoFilial f = InsumoFilial.novo(InsumoId.generate(), UUID.randomUUID(),
                BigDecimal.ZERO, null, null, T0);
        f.atualizarParametros(new BigDecimal("5"), new BigDecimal("50"), new BigDecimal("10"), T0);
        assertThat(f.estoqueMinimo()).isEqualByComparingTo("5");
        assertThat(f.estoqueMaximo()).contains(new BigDecimal("50"));
    }

    @Test
    void desativarEAtivar() {
        InsumoFilial f = InsumoFilial.novo(InsumoId.generate(), UUID.randomUUID(),
                null, null, null, T0);
        f.desativar(T0);
        assertThat(f.ativo()).isFalse();
        f.ativar(T0);
        assertThat(f.ativo()).isTrue();
    }

    @Test
    void rejeitaMinimoNegativo() {
        assertThatThrownBy(() -> InsumoFilial.novo(InsumoId.generate(), UUID.randomUUID(),
                new BigDecimal("-1"), null, null, T0))
                .isInstanceOf(ValidationException.class);
    }
}
