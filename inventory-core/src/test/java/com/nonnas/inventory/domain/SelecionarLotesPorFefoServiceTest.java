package com.nonnas.inventory.domain;

import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 7 cenários FEFO (master doc T04 acceptance: 5+):
 * 1. Um único lote, quantidade exata.
 * 2. Múltiplos lotes — consome o de validade mais próxima primeiro.
 * 3. Lote sem validade vai por último (NULLS LAST).
 * 4. Quantidade que esgota o primeiro e parcial no segundo.
 * 5. Saldo insuficiente — gera negativo no último, flag true.
 * 6. Sem lote algum — semLotes() true.
 * 7. Quantidade <= 0 rejeitada.
 */
class SelecionarLotesPorFefoServiceTest {

    private final SaldoLoteRepository repo = mock(SaldoLoteRepository.class);
    private final SelecionarLotesPorFefoService service = new SelecionarLotesPorFefoService(repo);

    private final UUID INSUMO = UUID.randomUUID();
    private final UUID FILIAL = UUID.randomUUID();
    private final LoteId L1 = LoteId.generate();
    private final LoteId L2 = LoteId.generate();
    private final LoteId L3 = LoteId.generate();

    @Test
    void cenario1_umLoteQuantidadeExata() {
        when(repo.findLotesParaSaidaFefo(INSUMO, FILIAL)).thenReturn(List.of(
                new SaldoLoteRepository.LoteSaldoFefo(L1, new BigDecimal("10"), LocalDate.of(2026, 6, 1))
        ));
        var r = service.selecionar(INSUMO, FILIAL, new BigDecimal("10"));
        assertThat(r.alocacoes()).hasSize(1);
        assertThat(r.alocacoes().get(0).loteId()).isEqualTo(L1);
        assertThat(r.alocacoes().get(0).quantidade()).isEqualByComparingTo("10");
        assertThat(r.gerouNegativo()).isFalse();
    }

    @Test
    void cenario2_doisLotesValidadeMaisProximaPrimeiro() {
        // Repository já retorna ordenado por validade ASC; teste apenas verifica
        // que o algoritmo respeita a ordem recebida.
        when(repo.findLotesParaSaidaFefo(INSUMO, FILIAL)).thenReturn(List.of(
                new SaldoLoteRepository.LoteSaldoFefo(L1, new BigDecimal("5"), LocalDate.of(2026, 6, 1)),
                new SaldoLoteRepository.LoteSaldoFefo(L2, new BigDecimal("10"), LocalDate.of(2026, 12, 1))
        ));
        var r = service.selecionar(INSUMO, FILIAL, new BigDecimal("8"));
        assertThat(r.alocacoes()).hasSize(2);
        assertThat(r.alocacoes().get(0).loteId()).isEqualTo(L1);
        assertThat(r.alocacoes().get(0).quantidade()).isEqualByComparingTo("5");
        assertThat(r.alocacoes().get(1).loteId()).isEqualTo(L2);
        assertThat(r.alocacoes().get(1).quantidade()).isEqualByComparingTo("3");
        assertThat(r.gerouNegativo()).isFalse();
    }

    @Test
    void cenario3_loteSemValidadeRespeitadoComoUltimo() {
        // Repository entrega L1 (com validade) primeiro e L2 (sem validade) depois,
        // por causa da query "ORDER BY data_validade NULLS LAST".
        when(repo.findLotesParaSaidaFefo(INSUMO, FILIAL)).thenReturn(List.of(
                new SaldoLoteRepository.LoteSaldoFefo(L1, new BigDecimal("3"), LocalDate.of(2026, 6, 1)),
                new SaldoLoteRepository.LoteSaldoFefo(L2, new BigDecimal("100"), null)
        ));
        var r = service.selecionar(INSUMO, FILIAL, new BigDecimal("50"));
        // L1 (3) consumido inteiro, depois 47 do L2.
        assertThat(r.alocacoes().get(0).loteId()).isEqualTo(L1);
        assertThat(r.alocacoes().get(0).quantidade()).isEqualByComparingTo("3");
        assertThat(r.alocacoes().get(1).loteId()).isEqualTo(L2);
        assertThat(r.alocacoes().get(1).quantidade()).isEqualByComparingTo("47");
    }

    @Test
    void cenario4_esgotaPrimeiroEParcialNoSegundo() {
        when(repo.findLotesParaSaidaFefo(INSUMO, FILIAL)).thenReturn(List.of(
                new SaldoLoteRepository.LoteSaldoFefo(L1, new BigDecimal("4"), LocalDate.of(2026, 6, 1)),
                new SaldoLoteRepository.LoteSaldoFefo(L2, new BigDecimal("20"), LocalDate.of(2026, 12, 1))
        ));
        var r = service.selecionar(INSUMO, FILIAL, new BigDecimal("4.0001"));
        assertThat(r.alocacoes()).hasSize(2);
        assertThat(r.alocacoes().get(1).quantidade()).isEqualByComparingTo("0.0001");
    }

    @Test
    void cenario5_saldoInsuficienteGeraNegativoNoUltimo() {
        when(repo.findLotesParaSaidaFefo(INSUMO, FILIAL)).thenReturn(List.of(
                new SaldoLoteRepository.LoteSaldoFefo(L1, new BigDecimal("3"), LocalDate.of(2026, 6, 1)),
                new SaldoLoteRepository.LoteSaldoFefo(L2, new BigDecimal("2"), LocalDate.of(2026, 12, 1))
        ));
        // Pede 10, total disponível = 5. Restante 5 vai pro último (L2) → negativo no save.
        var r = service.selecionar(INSUMO, FILIAL, new BigDecimal("10"));
        assertThat(r.gerouNegativo()).isTrue();
        assertThat(r.alocacoes()).hasSize(2);
        assertThat(r.alocacoes().get(0).quantidade()).isEqualByComparingTo("3");
        // L2 absorve 2 + restante 5 = 7 (saldo 2 - 7 = -5)
        assertThat(r.alocacoes().get(1).quantidade()).isEqualByComparingTo("7");
    }

    @Test
    void cenario6_semLotesRetornaSemLotes() {
        when(repo.findLotesParaSaidaFefo(INSUMO, FILIAL)).thenReturn(List.of());
        var r = service.selecionar(INSUMO, FILIAL, new BigDecimal("1"));
        assertThat(r.semLotes()).isTrue();
        assertThat(r.gerouNegativo()).isTrue();
    }

    @Test
    void cenario7_quantidadeNaoPositivaEhRejeitada() {
        assertThatThrownBy(() -> service.selecionar(INSUMO, FILIAL, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.selecionar(INSUMO, FILIAL, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
