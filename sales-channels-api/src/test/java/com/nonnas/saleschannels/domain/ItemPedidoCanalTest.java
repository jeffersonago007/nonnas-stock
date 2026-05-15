package com.nonnas.saleschannels.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemPedidoCanalTest {

    @Test
    void novoComCamposValidos() {
        ItemPedidoCanal i = ItemPedidoCanal.novo(
                1, "SKU1", "Refrigerante", new BigDecimal("2"), "UN",
                new BigDecimal("8.50"), new BigDecimal("17.00"), "sem gelo");
        assertThat(i.sequencia()).isEqualTo(1);
        assertThat(i.externalCodeOpt()).contains("SKU1");
        assertThat(i.quantidade()).isEqualByComparingTo("2");
        assertThat(i.precoTotal()).isEqualByComparingTo("17.00");
        assertThat(i.observacaoOpt()).contains("sem gelo");
        assertThat(i.produtoVendavelIdOpt()).isEmpty();
    }

    @Test
    void rejeitaSequenciaNaoPositiva() {
        assertThatThrownBy(() -> ItemPedidoCanal.novo(
                0, "x", "y", BigDecimal.ONE, "UN",
                BigDecimal.ONE, BigDecimal.ONE, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("sequência");
    }

    @Test
    void rejeitaQuantidadeNaoPositiva() {
        assertThatThrownBy(() -> ItemPedidoCanal.novo(
                1, "x", "y", BigDecimal.ZERO, "UN",
                BigDecimal.ONE, BigDecimal.ONE, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("quantidade");
    }

    @Test
    void rejeitaPrecoNegativo() {
        assertThatThrownBy(() -> ItemPedidoCanal.novo(
                1, "x", "y", BigDecimal.ONE, "UN",
                new BigDecimal("-1"), BigDecimal.ONE, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("precoUnitario");
    }

    @Test
    void resolverProdutoVendavelGuardaId() {
        ItemPedidoCanal i = ItemPedidoCanal.novo(
                1, "SKU1", "X", BigDecimal.ONE, "UN",
                BigDecimal.ONE, BigDecimal.ONE, null);
        UUID prodId = UUID.randomUUID();
        i.resolverProdutoVendavel(prodId);
        assertThat(i.produtoVendavelIdOpt()).contains(prodId);
    }
}
