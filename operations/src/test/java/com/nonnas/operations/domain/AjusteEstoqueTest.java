package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AjusteEstoqueTest {

    private final UUID filial = UUID.randomUUID();
    private final UUID insumo = UUID.randomUUID();
    private final UUID unidade = UUID.randomUUID();
    private final UUID solicitante = UUID.randomUUID();
    private final UUID gerente = UUID.randomUUID();
    private final BigDecimal threshold = new BigDecimal("50");
    private final Instant t0 = Instant.parse("2026-05-08T10:00:00Z");

    @Test
    void abaixoDoThreshold_criaAprovadoDireto() {
        var a = AjusteEstoque.novo(filial, insumo, unidade, new BigDecimal("10"),
                "perda no transporte", solicitante, threshold, null, t0);

        assertThat(a.requerAprovacao()).isFalse();
        assertThat(a.status()).isEqualTo(StatusAjuste.APROVADO);
        assertThat(a.dataAprovacaoOpt()).contains(t0);
    }

    @Test
    void acimaDoThreshold_criaPendente() {
        var a = AjusteEstoque.novo(filial, insumo, unidade, new BigDecimal("100"),
                "perda no transporte", solicitante, threshold, null, t0);

        assertThat(a.requerAprovacao()).isTrue();
        assertThat(a.status()).isEqualTo(StatusAjuste.PENDENTE_APROVACAO);
        assertThat(a.aprovadoPorOpt()).isEmpty();
    }

    @Test
    void diffNegativo_aplicaThresholdPorAbsoluto() {
        var a = AjusteEstoque.novo(filial, insumo, unidade, new BigDecimal("-100"),
                "perda", solicitante, threshold, null, t0);
        assertThat(a.requerAprovacao()).isTrue();
    }

    @Test
    void diffZero_lancaValidacao() {
        assertThatThrownBy(() -> AjusteEstoque.novo(filial, insumo, unidade, BigDecimal.ZERO,
                "x", solicitante, threshold, null, t0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void aprovar_apenasFromPendente() {
        var a = AjusteEstoque.novo(filial, insumo, unidade, new BigDecimal("100"),
                "perda", solicitante, threshold, null, t0);

        UUID movId = UUID.randomUUID();
        a.aprovar(gerente, movId, t0.plusSeconds(3600));

        assertThat(a.status()).isEqualTo(StatusAjuste.APROVADO);
        assertThat(a.aprovadoPorOpt()).contains(gerente);
        assertThat(a.movIdOpt()).contains(movId);

        // Re-aprovar falha
        assertThatThrownBy(() -> a.aprovar(gerente, UUID.randomUUID(), t0.plusSeconds(7200)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejeitar_apenasFromPendente() {
        var a = AjusteEstoque.novo(filial, insumo, unidade, new BigDecimal("100"),
                "perda", solicitante, threshold, null, t0);
        a.rejeitar(gerente, "número impreciso", t0.plusSeconds(3600));

        assertThat(a.status()).isEqualTo(StatusAjuste.REJEITADO);
        assertThat(a.rejeicaoMotivoOpt()).contains("número impreciso");
    }

    @Test
    void anexarMovimentacao_apenasUmaVez() {
        var a = AjusteEstoque.novo(filial, insumo, unidade, new BigDecimal("10"),
                "ok", solicitante, threshold, null, t0);
        UUID movId = UUID.randomUUID();
        a.anexarMovimentacao(movId, t0);
        assertThat(a.movIdOpt()).contains(movId);

        assertThatThrownBy(() -> a.anexarMovimentacao(UUID.randomUUID(), t0))
                .isInstanceOf(IllegalStateException.class);
    }
}
