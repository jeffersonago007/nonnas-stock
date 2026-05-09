package com.nonnas.reporting;

import com.nonnas.reporting.application.MovimentacaoPorPeriodoUseCase;
import com.nonnas.reporting.application.PosicaoEstoquePorFilialUseCase;
import com.nonnas.reporting.domain.PeriodoFiltro;
import com.nonnas.reporting.testsupport.AbstractReportingIntegrationTest;
import com.nonnas.reporting.testsupport.ReportingFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validação dos critérios de performance do master doc T08:
 *  - posição com 1000 lotes em < 500ms
 *  - consulta cross-tabela com 10k movimentações em < 500ms
 *
 * ADR 0010: substituímos o "Gatling micro" do master doc por JUnit + Duration
 * (equivalente para um único critério passa/falha; Gatling pode ser
 * reintroduzido em hardening final se métricas mais ricas forem necessárias).
 */
class ReportingPerformanceIT extends AbstractReportingIntegrationTest {

    private static final Duration LIMITE = Duration.ofMillis(500);

    @Autowired private ReportingFixtures fixtures;
    @Autowired private PosicaoEstoquePorFilialUseCase posicao;
    @Autowired private MovimentacaoPorPeriodoUseCase movimentacao;

    private UUID filial;
    private UUID usuario;
    private UUID kg;
    private UUID categoria;

    @BeforeEach
    void setUp() {
        fixtures.limparTudo();
        filial = UUID.randomUUID();
        usuario = UUID.randomUUID();
        kg = fixtures.idUnidadePadrao("KG");
        categoria = fixtures.criarCategoria("Performance");
    }

    @Test
    void posicaoEm1000Lotes_executa_em_menos_de_500ms() {
        UUID insumo = fixtures.criarInsumo(categoria, kg, "PERF-POS", "Insumo perf");
        fixtures.popularLotesEmLote(1000, insumo, filial, new BigDecimal("12.34"));

        // warm-up para descontar JIT e primeira leitura de cache
        posicao.execute(new PosicaoEstoquePorFilialUseCase.Filtros(filial, null), 0, 5000);

        long t0 = System.nanoTime();
        var resultado = posicao.execute(
                new PosicaoEstoquePorFilialUseCase.Filtros(filial, null), 0, 5000);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - t0);

        assertThat(resultado).isNotEmpty();
        assertThat(elapsed)
                .as("posicao com 1000 lotes (foi: %d ms)", elapsed.toMillis())
                .isLessThan(LIMITE);
    }

    @Test
    void movimentacaoPorPeriodoEm10kMovs_executa_em_menos_de_500ms() {
        UUID insumo = fixtures.criarInsumo(categoria, kg, "PERF-MOV", "Insumo movimentado");
        UUID lote = fixtures.criarLote(insumo, "PERF-LOTE", null, new BigDecimal("10.00"));
        fixtures.criarSaldo(lote, filial, new BigDecimal("100000"));
        fixtures.popularMovimentacoesEmLote(10_000, filial, usuario, insumo, lote, kg,
                new BigDecimal("10.00"));

        Instant inicio = Instant.now().minus(90, ChronoUnit.DAYS);
        Instant fim = Instant.now().plus(1, ChronoUnit.HOURS);
        var filtros = new MovimentacaoPorPeriodoUseCase.Filtros(
                filial, new PeriodoFiltro(inicio, fim), null);

        // warm-up
        movimentacao.execute(filtros, 0, 5000);

        long t0 = System.nanoTime();
        var resultado = movimentacao.execute(filtros, 0, 5000);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - t0);

        assertThat(resultado).isNotEmpty();
        assertThat(elapsed)
                .as("movimentacao_por_periodo com 10k movs (foi: %d ms)", elapsed.toMillis())
                .isLessThan(LIMITE);
    }
}
