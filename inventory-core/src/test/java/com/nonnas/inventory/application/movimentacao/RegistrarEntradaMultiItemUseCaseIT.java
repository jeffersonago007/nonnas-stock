package com.nonnas.inventory.application.movimentacao;

import com.nonnas.inventory.application.ports.LoteRepository;
import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import com.nonnas.inventory.domain.Lote;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.inventory.testsupport.AbstractInventoryIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IT para entrada multi-item — espelho do multi-item de saída (T05). Cria N
 * lotes novos + 1 movimentação consolidada em uma única transação.
 */
class RegistrarEntradaMultiItemUseCaseIT extends AbstractInventoryIntegrationTest {

    @Autowired
    private RegistrarEntradaMultiItemUseCase entradaMulti;

    @Autowired
    private LoteRepository loteRepo;

    @Autowired
    private SaldoLoteRepository saldoRepo;

    @Test
    void doisInsumosEmUmaSoMovimentacao_criaLotesEAtualizaSaldos() {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumoA = UUID.randomUUID();
        UUID insumoB = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();

        UUID origemTransfId = UUID.randomUUID();
        var cmd = new RegistrarEntradaMultiItemUseCase.Comando(
                filial, usuario, TipoMovimentacao.ENTRADA_TRANSFERENCIA,
                "TRANSFERENCIA", origemTransfId, "recebimento de transferência",
                List.of(
                        new RegistrarEntradaMultiItemUseCase.ItemEntrada(
                                insumoA, null, null, "MUSS-001",
                                LocalDate.parse("2026-04-01"), LocalDate.parse("2026-09-01"),
                                new BigDecimal("32.50"), unidade,
                                new BigDecimal("10"), new BigDecimal("10"), true),
                        new RegistrarEntradaMultiItemUseCase.ItemEntrada(
                                insumoB, null, null, "MOLHO-001",
                                null, LocalDate.parse("2026-12-01"),
                                new BigDecimal("18.00"), unidade,
                                new BigDecimal("5"), new BigDecimal("5"), true)
                )
        );

        Movimentacao mov = entradaMulti.execute(cmd);

        assertThat(mov.itens()).hasSize(2);
        assertThat(mov.tipo()).isEqualTo(TipoMovimentacao.ENTRADA_TRANSFERENCIA);
        assertThat(mov.documentoOrigemTipoOpt()).contains("TRANSFERENCIA");
        assertThat(mov.documentoOrigemIdOpt()).contains(origemTransfId);
        assertThat(mov.gerouNegativo()).isFalse();

        assertThat(saldoRepo.somarPorInsumoEFilial(insumoA, filial)).isEqualByComparingTo("10");
        assertThat(saldoRepo.somarPorInsumoEFilial(insumoB, filial)).isEqualByComparingTo("5");
    }

    @Test
    void tipoSaida_lancaValidacao() {
        var cmd = new RegistrarEntradaMultiItemUseCase.Comando(
                UUID.randomUUID(), UUID.randomUUID(), TipoMovimentacao.SAIDA_VENDA,
                null, null, null,
                List.of(new RegistrarEntradaMultiItemUseCase.ItemEntrada(
                        UUID.randomUUID(), null, null, "L1",
                        null, null, BigDecimal.TEN, UUID.randomUUID(),
                        BigDecimal.ONE, BigDecimal.ONE, true))
        );

        assertThatThrownBy(() -> entradaMulti.execute(cmd))
                .hasMessageContaining("entrada");
    }

    @Test
    void itensVazios_lancaValidacao() {
        var cmd = new RegistrarEntradaMultiItemUseCase.Comando(
                UUID.randomUUID(), UUID.randomUUID(), TipoMovimentacao.ENTRADA_AJUSTE,
                null, null, null, List.of()
        );

        assertThatThrownBy(() -> entradaMulti.execute(cmd))
                .hasMessageContaining("obrigatório");
    }

    @Test
    void agregador_duasEntradas_calculaCustoMedioPonderado() {
        // T-CMV-01: insumo sem controla_validade vai pro lote AGREGADOR.
        // Custo médio = (10×20 + 10×30) / 20 = 25,00.
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();

        var entrada1 = new RegistrarEntradaMultiItemUseCase.Comando(
                filial, usuario, TipoMovimentacao.ENTRADA_AJUSTE, null, null, null,
                List.of(new RegistrarEntradaMultiItemUseCase.ItemEntrada(
                        insumo, null, null, null, null, null,
                        new BigDecimal("20.0000"), unidade,
                        new BigDecimal("10"), new BigDecimal("10"), false)));
        entradaMulti.execute(entrada1);

        Lote apos1 = loteRepo.findAgregadorByInsumo(insumo).orElseThrow();
        assertThat(apos1.valorUnitario()).isEqualByComparingTo("20.0000");

        var entrada2 = new RegistrarEntradaMultiItemUseCase.Comando(
                filial, usuario, TipoMovimentacao.ENTRADA_AJUSTE, null, null, null,
                List.of(new RegistrarEntradaMultiItemUseCase.ItemEntrada(
                        insumo, null, null, null, null, null,
                        new BigDecimal("30.0000"), unidade,
                        new BigDecimal("10"), new BigDecimal("10"), false)));
        entradaMulti.execute(entrada2);

        Lote apos2 = loteRepo.findAgregadorByInsumo(insumo).orElseThrow();
        assertThat(apos2.id()).isEqualTo(apos1.id()); // mesmo lote, valor atualizado
        assertThat(apos2.valorUnitario()).isEqualByComparingTo("25.0000");
    }

    @Test
    void numeroLoteEmBranco_lancaValidacao() {
        var cmd = new RegistrarEntradaMultiItemUseCase.Comando(
                UUID.randomUUID(), UUID.randomUUID(), TipoMovimentacao.ENTRADA_AJUSTE,
                null, null, null,
                List.of(new RegistrarEntradaMultiItemUseCase.ItemEntrada(
                        UUID.randomUUID(), null, null, "  ",
                        null, null, BigDecimal.TEN, UUID.randomUUID(),
                        BigDecimal.ONE, BigDecimal.ONE, true))
        );

        assertThatThrownBy(() -> entradaMulti.execute(cmd))
                .hasMessageContaining("Número do lote");
    }
}
