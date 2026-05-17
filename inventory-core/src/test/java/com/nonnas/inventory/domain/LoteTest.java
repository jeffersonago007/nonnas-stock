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
    void novoRastreadoValido() {
        Lote l = Lote.novoRastreado(UUID.randomUUID(), null, null, "L-001",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 12, 1),
                new BigDecimal("25.0000"), T0);
        assertThat(l.tipo()).isEqualTo(TipoLote.RASTREADO);
        assertThat(l.numeroLote()).isEqualTo("L-001");
        assertThat(l.fornecedorIdOpt()).isEmpty();
        assertThat(l.dataValidadeOpt()).contains(LocalDate.of(2026, 12, 1));
    }

    @Test
    void rejeitaNumeroLoteVazio() {
        assertThatThrownBy(() -> Lote.novoRastreado(UUID.randomUUID(), null, null, "  ",
                null, null, new BigDecimal("1"), T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaValorNegativo() {
        assertThatThrownBy(() -> Lote.novoRastreado(UUID.randomUUID(), null, null, "L-001",
                null, null, new BigDecimal("-1"), T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaValidadeAntesDaFabricacao() {
        assertThatThrownBy(() -> Lote.novoRastreado(UUID.randomUUID(), null, null, "L-001",
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 5, 1),
                BigDecimal.ONE, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void novoAgregadorVemSemNumeroDatasFornecedor() {
        UUID insumoId = UUID.randomUUID();
        Lote l = Lote.novoAgregador(insumoId, T0);
        assertThat(l.tipo()).isEqualTo(TipoLote.AGREGADOR);
        assertThat(l.insumoId()).isEqualTo(insumoId);
        assertThat(l.numeroLoteOpt()).isEmpty();
        assertThat(l.dataFabricacaoOpt()).isEmpty();
        assertThat(l.dataValidadeOpt()).isEmpty();
        assertThat(l.fornecedorIdOpt()).isEmpty();
        assertThat(l.notaFiscalIdOpt()).isEmpty();
        assertThat(l.valorUnitario()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void agregadorAceitaNovoValorUnitario() {
        Lote l = Lote.novoAgregador(UUID.randomUUID(), T0);
        Lote atualizado = l.comNovoValorUnitarioAgregador(new BigDecimal("12.3400"));
        assertThat(atualizado.id()).isEqualTo(l.id());
        assertThat(atualizado.tipo()).isEqualTo(TipoLote.AGREGADOR);
        assertThat(atualizado.valorUnitario()).isEqualByComparingTo("12.3400");
    }

    @Test
    void rastreadoRejeitaAtualizacaoDeValorUnitario() {
        Lote l = Lote.novoRastreado(UUID.randomUUID(), null, null, "L-001",
                null, null, new BigDecimal("5.0000"), T0);
        assertThatThrownBy(() -> l.comNovoValorUnitarioAgregador(new BigDecimal("9")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("AGREGADOR");
    }

    @Test
    void agregadorRejeitaNovoValorNegativo() {
        Lote l = Lote.novoAgregador(UUID.randomUUID(), T0);
        assertThatThrownBy(() -> l.comNovoValorUnitarioAgregador(new BigDecimal("-1")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void agregadorRejeitaCamposDeRastreio() {
        UUID id = UUID.randomUUID();
        Instant now = T0;
        // Construtor direto com tipo AGREGADOR + numero não pode existir.
        assertThatThrownBy(() -> new Lote(LoteId.generate(), id, TipoLote.AGREGADOR,
                null, null, "L-XYZ", null, null, BigDecimal.ZERO, now))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("número");
        assertThatThrownBy(() -> new Lote(LoteId.generate(), id, TipoLote.AGREGADOR,
                null, null, null, LocalDate.of(2026, 5, 1), null, BigDecimal.ZERO, now))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Lote(LoteId.generate(), id, TipoLote.AGREGADOR,
                null, null, null, null, LocalDate.of(2026, 12, 1), BigDecimal.ZERO, now))
                .isInstanceOf(ValidationException.class);
    }
}
