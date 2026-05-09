package com.nonnas.operations.application.ajuste;

import com.nonnas.inventory.application.movimentacao.RegistrarEntradaManualUseCase;
import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.operations.domain.AjusteEstoque;
import com.nonnas.operations.domain.StatusAjuste;
import com.nonnas.operations.testsupport.AbstractOperationsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AjusteEstoqueIT extends AbstractOperationsIntegrationTest {

    @Autowired private LancarAjusteManualUseCase lancar;
    @Autowired private AprovarAjusteEstoqueUseCase aprovar;
    @Autowired private RegistrarEntradaManualUseCase entradaInv;
    @Autowired private SaldoLoteRepository saldoRepo;

    @Test
    void abaixoThreshold_geraMovimentacaoDireto_aumentaSaldo() {
        UUID filial = UUID.randomUUID();
        UUID solicitante = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID kg = UUID.randomUUID();

        AjusteEstoque a = lancar.execute(new LancarAjusteManualUseCase.Comando(
                filial, insumo, kg, new BigDecimal("10"), "ajuste de inventário", solicitante));

        assertThat(a.status()).isEqualTo(StatusAjuste.APROVADO);
        assertThat(a.movIdOpt()).isPresent();
        assertThat(saldoRepo.somarPorInsumoEFilial(insumo, filial)).isEqualByComparingTo("10");
    }

    @Test
    void acimaThreshold_ficaPendente_naoMexeEstoqueAteAprovar() {
        UUID filial = UUID.randomUUID();
        UUID solicitante = UUID.randomUUID();
        UUID gerente = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID kg = UUID.randomUUID();

        AjusteEstoque a = lancar.execute(new LancarAjusteManualUseCase.Comando(
                filial, insumo, kg, new BigDecimal("100"), "diferença grande", solicitante));

        assertThat(a.status()).isEqualTo(StatusAjuste.PENDENTE_APROVACAO);
        assertThat(a.movIdOpt()).isEmpty();
        assertThat(saldoRepo.somarPorInsumoEFilial(insumo, filial)).isEqualByComparingTo("0");

        AjusteEstoque aprovado = aprovar.execute(a.id().value(), gerente);
        assertThat(aprovado.status()).isEqualTo(StatusAjuste.APROVADO);
        assertThat(aprovado.movIdOpt()).isPresent();
        assertThat(saldoRepo.somarPorInsumoEFilial(insumo, filial)).isEqualByComparingTo("100");
    }

    @Test
    void ajusteNegativo_baixaPorFefo() {
        UUID filial = UUID.randomUUID();
        UUID solicitante = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID kg = UUID.randomUUID();

        // Estoque pré-existente: 30 unidades
        var ent = new RegistrarEntradaManualUseCase.Comando(
                filial, solicitante, insumo, null, null, "L-AJ-1",
                null, LocalDate.parse("2026-12-01"), new BigDecimal("10"),
                kg, new BigDecimal("30"), new BigDecimal("30"),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null);
        entradaInv.execute(ent);

        AjusteEstoque a = lancar.execute(new LancarAjusteManualUseCase.Comando(
                filial, insumo, kg, new BigDecimal("-5"), "perda detectada", solicitante));

        assertThat(a.status()).isEqualTo(StatusAjuste.APROVADO);
        assertThat(saldoRepo.somarPorInsumoEFilial(insumo, filial)).isEqualByComparingTo("25");
    }
}
