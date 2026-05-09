package com.nonnas.inventory.application.ports;

import com.nonnas.inventory.application.movimentacao.RegistrarEntradaManualUseCase;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.inventory.testsupport.AbstractInventoryIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre {@link SaldoLoteRepository#findLotesVencendoComSaldoAte(LocalDate)}
 * — query usada pelo job diário de alertas de vencimento (T07).
 */
class SaldoLoteVencimentoQueryIT extends AbstractInventoryIntegrationTest {

    @Autowired private SaldoLoteRepository saldoRepo;
    @Autowired private RegistrarEntradaManualUseCase entrada;

    @Test
    void retornaApenasLotesVencendoComSaldoPositivo() {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID kg = UUID.randomUUID();
        LocalDate hoje = LocalDate.parse("2026-05-08");

        // Lote vencendo dentro da janela
        criarLote(filial, usuario, insumo, kg, "L-VENC", hoje.plusDays(3), 10);
        // Lote vencendo fora da janela
        criarLote(filial, usuario, insumo, kg, "L-LONGE", hoje.plusDays(60), 5);
        // Lote sem validade não aparece
        criarLote(filial, usuario, insumo, kg, "L-SEM-VAL", null, 7);

        var resultado = saldoRepo.findLotesVencendoComSaldoAte(hoje.plusDays(7));

        assertThat(resultado).hasSize(1);
        var primeiro = resultado.get(0);
        assertThat(primeiro.insumoId()).isEqualTo(insumo);
        assertThat(primeiro.filialId()).isEqualTo(filial);
        assertThat(primeiro.saldoBase()).isEqualByComparingTo("10");
        assertThat(primeiro.dataValidade()).isEqualTo(hoje.plusDays(3));
    }

    private void criarLote(UUID filial, UUID usuario, UUID insumo, UUID unidade,
                           String numeroLote, LocalDate validade, int qtd) {
        var cmd = new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumo, null, null, numeroLote,
                null, validade, BigDecimal.TEN,
                unidade, new BigDecimal(qtd), new BigDecimal(qtd),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null);
        entrada.execute(cmd);
    }
}
