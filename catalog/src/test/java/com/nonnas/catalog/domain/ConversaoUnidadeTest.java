package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversaoUnidadeTest {

    private static final Instant T0 = Instant.parse("2026-05-08T12:00:00Z");

    @Test
    void globalNaoTemInsumoId() {
        ConversaoUnidade c = ConversaoUnidade.global(
                UnidadeMedidaId.generate(), UnidadeMedidaId.generate(),
                new BigDecimal("1000"), T0);
        assertThat(c.isGlobal()).isTrue();
        assertThat(c.insumoIdOpt()).isEmpty();
    }

    @Test
    void porInsumoTemInsumoId() {
        InsumoId iId = InsumoId.generate();
        ConversaoUnidade c = ConversaoUnidade.porInsumo(
                UnidadeMedidaId.generate(), UnidadeMedidaId.generate(),
                new BigDecimal("5"), iId, T0);
        assertThat(c.isGlobal()).isFalse();
        assertThat(c.insumoIdOpt()).contains(iId);
    }

    @Test
    void rejeitaOrigemIgualDestino() {
        UnidadeMedidaId u = UnidadeMedidaId.generate();
        assertThatThrownBy(() -> ConversaoUnidade.global(u, u, BigDecimal.ONE, T0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("diferentes");
    }

    @Test
    void rejeitaFatorZeroOuNegativo() {
        UnidadeMedidaId origem = UnidadeMedidaId.generate();
        UnidadeMedidaId destino = UnidadeMedidaId.generate();
        assertThatThrownBy(() -> ConversaoUnidade.global(origem, destino, BigDecimal.ZERO, T0))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> ConversaoUnidade.global(origem, destino, new BigDecimal("-1"), T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void porInsumoRejeitaInsumoIdNulo() {
        UnidadeMedidaId origem = UnidadeMedidaId.generate();
        UnidadeMedidaId destino = UnidadeMedidaId.generate();
        assertThatThrownBy(() -> ConversaoUnidade.porInsumo(origem, destino, BigDecimal.ONE, null, T0))
                .isInstanceOf(NullPointerException.class);
    }
}
