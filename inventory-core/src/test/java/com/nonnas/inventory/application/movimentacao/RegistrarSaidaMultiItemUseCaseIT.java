package com.nonnas.inventory.application.movimentacao;

import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.inventory.testsupport.AbstractInventoryIntegrationTest;
import com.nonnas.sharedkernel.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IT para o use case multi-item — uma única movimentação SAIDA_VENDA
 * baixando múltiplos insumos via FEFO, todos na mesma transação.
 */
class RegistrarSaidaMultiItemUseCaseIT extends AbstractInventoryIntegrationTest {

    @Autowired
    private RegistrarSaidaMultiItemUseCase saidaMulti;

    @Autowired
    private RegistrarEntradaManualUseCase entrada;

    @Autowired
    private SaldoLoteRepository saldoRepo;

    @Test
    void doisInsumosBaixadosViaFefoEmUmaSoMovimentacao() {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumoA = UUID.randomUUID();
        UUID insumoB = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();

        criarEntrada(filial, usuario, insumoA, unidade, "A-1", LocalDate.parse("2026-06-01"), 5);
        criarEntrada(filial, usuario, insumoA, unidade, "A-2", LocalDate.parse("2026-12-01"), 10);
        criarEntrada(filial, usuario, insumoB, unidade, "B-1", LocalDate.parse("2026-09-01"), 8);

        UUID fichaId = UUID.randomUUID();
        var cmd = new RegistrarSaidaMultiItemUseCase.Comando(
                filial, usuario, TipoMovimentacao.SAIDA_VENDA,
                "FICHA_TECNICA", fichaId, "venda simulada",
                List.of(
                        new RegistrarSaidaMultiItemUseCase.ItemSaida(insumoA, unidade, new BigDecimal("7")),
                        new RegistrarSaidaMultiItemUseCase.ItemSaida(insumoB, unidade, new BigDecimal("3"))
                )
        );

        Movimentacao mov = saidaMulti.execute(cmd);

        // 3 itens: 2 lotes do insumoA (5+2) + 1 lote do insumoB (3).
        assertThat(mov.itens()).hasSize(3);
        assertThat(mov.tipo()).isEqualTo(TipoMovimentacao.SAIDA_VENDA);
        assertThat(mov.documentoOrigemTipoOpt()).contains("FICHA_TECNICA");
        assertThat(mov.documentoOrigemIdOpt()).contains(fichaId);
        assertThat(mov.gerouNegativo()).isFalse();

        assertThat(saldoRepo.somarPorInsumoEFilial(insumoA, filial)).isEqualByComparingTo("8");  // 15 - 7
        assertThat(saldoRepo.somarPorInsumoEFilial(insumoB, filial)).isEqualByComparingTo("5");  // 8 - 3
    }

    @Test
    void semLoteEmUmDosInsumos_rolaBackTudo() {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumoComLote = UUID.randomUUID();
        UUID insumoSemLote = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();

        criarEntrada(filial, usuario, insumoComLote, unidade, "X-1", LocalDate.parse("2026-12-01"), 10);

        var cmd = new RegistrarSaidaMultiItemUseCase.Comando(
                filial, usuario, TipoMovimentacao.SAIDA_VENDA,
                "FICHA_TECNICA", UUID.randomUUID(), null,
                List.of(
                        new RegistrarSaidaMultiItemUseCase.ItemSaida(insumoComLote, unidade, new BigDecimal("2")),
                        new RegistrarSaidaMultiItemUseCase.ItemSaida(insumoSemLote, unidade, new BigDecimal("1"))
                )
        );

        assertThatThrownBy(() -> saidaMulti.execute(cmd))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Sem lote disponível");

        // Saldo do insumo que tinha lote permanece intacto: rollback funcionou.
        assertThat(saldoRepo.somarPorInsumoEFilial(insumoComLote, filial)).isEqualByComparingTo("10");
    }

    @Test
    void insumoDuplicadoNoComando_retornaErroValidacao() {
        UUID insumo = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();
        var cmd = new RegistrarSaidaMultiItemUseCase.Comando(
                UUID.randomUUID(), UUID.randomUUID(), TipoMovimentacao.SAIDA_VENDA,
                "FICHA_TECNICA", UUID.randomUUID(), null,
                List.of(
                        new RegistrarSaidaMultiItemUseCase.ItemSaida(insumo, unidade, new BigDecimal("1")),
                        new RegistrarSaidaMultiItemUseCase.ItemSaida(insumo, unidade, new BigDecimal("2"))
                )
        );

        assertThatThrownBy(() -> saidaMulti.execute(cmd))
                .hasMessageContaining("duplicado");
    }

    private void criarEntrada(UUID filial, UUID usuario, UUID insumo, UUID unidade,
                              String numeroLote, LocalDate validade, int qtd) {
        var cmd = new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumo, null, null, numeroLote,
                null, validade, new BigDecimal("10.00"),
                unidade, new BigDecimal(qtd), new BigDecimal(qtd),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null);
        entrada.execute(cmd);
    }
}
