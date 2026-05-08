package com.nonnas.inventory.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovimentacaoTest {

    private static final Instant T0 = Instant.parse("2026-05-08T12:00:00Z");

    private ItemMovimentacao item() {
        return ItemMovimentacao.novo(UUID.randomUUID(), LoteId.generate(), UUID.randomUUID(),
                BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("10"));
    }

    @Test
    void movimentacaoEhImutavelLista() {
        ItemMovimentacao i = item();
        java.util.List<ItemMovimentacao> mutavel = new java.util.ArrayList<>();
        mutavel.add(i);
        Movimentacao m = Movimentacao.nova(UUID.randomUUID(), UUID.randomUUID(),
                TipoMovimentacao.ENTRADA_AJUSTE, T0, null, null, null, false, mutavel, T0);
        // Mutar a lista original não afeta a entidade — defensive copy
        mutavel.clear();
        assertThat(m.itens()).hasSize(1);
        // E itens() retornado é unmodifiable
        assertThatThrownBy(() -> m.itens().add(item()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejeitaSemItens() {
        assertThatThrownBy(() -> Movimentacao.nova(UUID.randomUUID(), UUID.randomUUID(),
                TipoMovimentacao.ENTRADA_AJUSTE, T0, null, null, null, false, List.of(), T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void tipoEntradaESaida() {
        assertThat(TipoMovimentacao.ENTRADA_NF.isEntrada()).isTrue();
        assertThat(TipoMovimentacao.ENTRADA_NF.isSaida()).isFalse();
        assertThat(TipoMovimentacao.SAIDA_VENDA.isSaida()).isTrue();
        assertThat(TipoMovimentacao.SAIDA_VENDA.isEntrada()).isFalse();
    }

    @Test
    void itemRejeitaQuantidadeZero() {
        assertThatThrownBy(() -> ItemMovimentacao.novo(UUID.randomUUID(), LoteId.generate(),
                UUID.randomUUID(), BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN))
                .isInstanceOf(ValidationException.class);
    }
}
