package com.nonnas.reporting;

import com.nonnas.reporting.application.CurvaABCUseCase;
import com.nonnas.reporting.application.DivergenciaInventarioUseCase;
import com.nonnas.reporting.application.MovimentacaoPorPeriodoUseCase;
import com.nonnas.reporting.application.PosicaoEstoquePorFilialUseCase;
import com.nonnas.reporting.application.RupturaIminenteUseCase;
import com.nonnas.reporting.application.VencimentoProximoUseCase;
import com.nonnas.reporting.domain.ClasseABC;
import com.nonnas.reporting.domain.PeriodoFiltro;
import com.nonnas.reporting.domain.SituacaoRuptura;
import com.nonnas.reporting.testsupport.AbstractReportingIntegrationTest;
import com.nonnas.reporting.testsupport.ReportingFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportingRelatoriosIT extends AbstractReportingIntegrationTest {

    @Autowired private ReportingFixtures fixtures;
    @Autowired private PosicaoEstoquePorFilialUseCase posicao;
    @Autowired private CurvaABCUseCase curvaAbc;
    @Autowired private RupturaIminenteUseCase ruptura;
    @Autowired private VencimentoProximoUseCase vencimento;
    @Autowired private MovimentacaoPorPeriodoUseCase movimentacao;
    @Autowired private DivergenciaInventarioUseCase divergencia;

    private UUID kg;
    private UUID centro;
    private UUID outraFilial;
    private UUID usuario;
    private UUID categoria;

    @BeforeEach
    void setUp() {
        fixtures.limparTudo();
        kg = fixtures.idUnidadePadrao("KG");
        centro = UUID.randomUUID();
        outraFilial = UUID.randomUUID();
        usuario = UUID.randomUUID();
        categoria = fixtures.criarCategoria("Laticínios");
    }

    @Test
    void posicaoEstoque_agregaSaldoEValorPorInsumoNaFilial() {
        UUID mussarela = fixtures.criarInsumo(categoria, kg, "MUS-001", "Mussarela");
        UUID parmesao = fixtures.criarInsumo(categoria, kg, "PAR-001", "Parmesão");

        UUID lote1 = fixtures.criarLote(mussarela, "L1", LocalDate.now().plusMonths(2), new BigDecimal("30.00"));
        UUID lote2 = fixtures.criarLote(mussarela, "L2", LocalDate.now().plusMonths(3), new BigDecimal("32.00"));
        UUID lote3 = fixtures.criarLote(parmesao,  "P1", LocalDate.now().plusMonths(1), new BigDecimal("80.00"));

        fixtures.criarSaldo(lote1, centro, new BigDecimal("10"));
        fixtures.criarSaldo(lote2, centro, new BigDecimal("5"));
        fixtures.criarSaldo(lote3, centro, new BigDecimal("4"));
        // saldo zerado em outra filial — não aparece pra "centro"
        fixtures.criarSaldo(lote1, outraFilial, new BigDecimal("100"));

        var resultado = posicao.execute(
                new PosicaoEstoquePorFilialUseCase.Filtros(centro, null), 0, 50);

        assertThat(resultado).hasSize(2);
        var mus = resultado.stream().filter(r -> r.insumoCodigo().equals("MUS-001")).findFirst().orElseThrow();
        assertThat(mus.saldoTotal()).isEqualByComparingTo("15");
        assertThat(mus.valorEstoque()).isEqualByComparingTo("460"); // 10*30 + 5*32
        assertThat(mus.quantidadeLotes()).isEqualTo(2);
    }

    @Test
    void curvaABC_classificaPorValorConsumido_AAtinge80PorcentoBAte95() {
        // Cria 5 insumos com diferentes níveis de consumo numa filial.
        // Valores escolhidos para queda nítida em A/B/C.
        UUID i1 = fixtures.criarInsumo(categoria, kg, "X-01", "Item 1"); // 50% do consumo
        UUID i2 = fixtures.criarInsumo(categoria, kg, "X-02", "Item 2"); // 30%
        UUID i3 = fixtures.criarInsumo(categoria, kg, "X-03", "Item 3"); // 12%
        UUID i4 = fixtures.criarInsumo(categoria, kg, "X-04", "Item 4"); // 5%
        UUID i5 = fixtures.criarInsumo(categoria, kg, "X-05", "Item 5"); // 3%

        registraSaida(centro, i1, new BigDecimal("100"), new BigDecimal("5.00"));   // 500
        registraSaida(centro, i2, new BigDecimal("100"), new BigDecimal("3.00"));   // 300
        registraSaida(centro, i3, new BigDecimal("100"), new BigDecimal("1.20"));   // 120
        registraSaida(centro, i4, new BigDecimal("100"), new BigDecimal("0.50"));   //  50
        registraSaida(centro, i5, new BigDecimal("100"), new BigDecimal("0.30"));   //  30

        fixtures.refreshViewsMaterializadas();

        var resultado = curvaAbc.execute(centro, 0, 50);
        assertThat(resultado).hasSize(5);

        // i1 e i2 cobrem 80% (500+300 = 800 / 1000) → A
        assertThat(curvaPorCodigo(resultado, "X-01").classe()).isEqualTo(ClasseABC.A);
        assertThat(curvaPorCodigo(resultado, "X-02").classe()).isEqualTo(ClasseABC.A);
        // i3 entra na faixa B (acumulado 92% ≤ 95%)
        assertThat(curvaPorCodigo(resultado, "X-03").classe()).isEqualTo(ClasseABC.B);
        // i4 e i5 são C
        assertThat(curvaPorCodigo(resultado, "X-04").classe()).isEqualTo(ClasseABC.C);
        assertThat(curvaPorCodigo(resultado, "X-05").classe()).isEqualTo(ClasseABC.C);
    }

    @Test
    void rupturaIminente_classifica_RUPTURA_TOTAL_e_ABAIXO_PONTO_PEDIDO() {
        UUID semSaldo = fixtures.criarInsumo(categoria, kg, "VAZ-01", "Sem saldo");
        UUID baixo   = fixtures.criarInsumo(categoria, kg, "BAI-01", "Abaixo ponto");
        UUID ok      = fixtures.criarInsumo(categoria, kg, "OK-01", "Saldo OK");

        fixtures.criarInsumoFilial(semSaldo, centro, new BigDecimal("0"),  new BigDecimal("5"));
        fixtures.criarInsumoFilial(baixo,    centro, new BigDecimal("0"),  new BigDecimal("10"));
        fixtures.criarInsumoFilial(ok,       centro, new BigDecimal("0"),  new BigDecimal("5"));

        UUID lb = fixtures.criarLote(baixo, "B1", LocalDate.now().plusMonths(2), new BigDecimal("20.00"));
        fixtures.criarSaldo(lb, centro, new BigDecimal("3")); // abaixo do ponto_pedido=10
        UUID lok = fixtures.criarLote(ok, "K1", LocalDate.now().plusMonths(2), new BigDecimal("20.00"));
        fixtures.criarSaldo(lok, centro, new BigDecimal("100")); // muito acima, fora da MV

        fixtures.refreshViewsMaterializadas();

        var resultado = ruptura.execute(centro, 0, 50);

        assertThat(resultado).hasSize(2);
        var totalmente = resultado.stream()
                .filter(r -> r.insumoCodigo().equals("VAZ-01")).findFirst().orElseThrow();
        assertThat(totalmente.situacao()).isEqualTo(SituacaoRuptura.RUPTURA_TOTAL);
        var abaixo = resultado.stream()
                .filter(r -> r.insumoCodigo().equals("BAI-01")).findFirst().orElseThrow();
        assertThat(abaixo.situacao()).isEqualTo(SituacaoRuptura.ABAIXO_PONTO_PEDIDO);
    }

    @Test
    void vencimentoProximo_listaLotesDentroDaJanela_ordenadosPorValidadeAsc() {
        UUID insumo = fixtures.criarInsumo(categoria, kg, "VEN-01", "Vence em breve");

        UUID l3dias = fixtures.criarLote(insumo, "V3", LocalDate.now().plusDays(3), new BigDecimal("10.00"));
        UUID l10dias = fixtures.criarLote(insumo, "V10", LocalDate.now().plusDays(10), new BigDecimal("10.00"));
        UUID l60dias = fixtures.criarLote(insumo, "V60", LocalDate.now().plusDays(60), new BigDecimal("10.00"));

        fixtures.criarSaldo(l3dias, centro, new BigDecimal("5"));
        fixtures.criarSaldo(l10dias, centro, new BigDecimal("5"));
        fixtures.criarSaldo(l60dias, centro, new BigDecimal("5"));

        var resultado = vencimento.execute(centro, 30, 0, 50);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).numeroLote()).isEqualTo("V3");
        assertThat(resultado.get(1).numeroLote()).isEqualTo("V10");
        assertThat(resultado.get(0).diasParaVencer()).isLessThanOrEqualTo(3);
    }

    @Test
    void vencimentoProximo_ignoraLotesComSaldoZero() {
        UUID insumo = fixtures.criarInsumo(categoria, kg, "VEN-Z", "Lote zerado");
        UUID lote = fixtures.criarLote(insumo, "Z1", LocalDate.now().plusDays(2), new BigDecimal("10.00"));
        fixtures.criarSaldo(lote, centro, BigDecimal.ZERO);

        var resultado = vencimento.execute(centro, 30, 0, 50);

        assertThat(resultado).isEmpty();
    }

    @Test
    void movimentacaoPorPeriodo_agrupaPorInsumoFilialETipo_dentroDoPeriodo() {
        UUID insumo = fixtures.criarInsumo(categoria, kg, "MOV-01", "Insumo movimentado");
        UUID lote = fixtures.criarLote(insumo, "M1", LocalDate.now().plusMonths(2), new BigDecimal("10.00"));
        fixtures.criarSaldo(lote, centro, new BigDecimal("100"));

        Instant inicio = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant fim = Instant.now().plus(1, ChronoUnit.HOURS);

        Instant ontem = Instant.now().minus(1, ChronoUnit.DAYS);
        UUID mov1 = fixtures.criarMovimentacao(centro, usuario, "SAIDA_VENDA", ontem);
        fixtures.criarItemMovimentacao(mov1, insumo, lote, kg, new BigDecimal("5"), new BigDecimal("10.00"));

        UUID mov2 = fixtures.criarMovimentacao(centro, usuario, "SAIDA_VENDA", ontem);
        fixtures.criarItemMovimentacao(mov2, insumo, lote, kg, new BigDecimal("3"), new BigDecimal("10.00"));

        // fora do período — não conta
        UUID antiga = fixtures.criarMovimentacao(centro, usuario, "SAIDA_VENDA",
                Instant.now().minus(60, ChronoUnit.DAYS));
        fixtures.criarItemMovimentacao(antiga, insumo, lote, kg, new BigDecimal("99"), new BigDecimal("10.00"));

        var resultado = movimentacao.execute(
                new MovimentacaoPorPeriodoUseCase.Filtros(centro, new PeriodoFiltro(inicio, fim), "SAIDA_VENDA"),
                0, 50);

        assertThat(resultado).hasSize(1);
        var item = resultado.get(0);
        assertThat(item.tipoMovimentacao()).isEqualTo("SAIDA_VENDA");
        assertThat(item.quantidadeMovimentacoes()).isEqualTo(2);
        assertThat(item.quantidadeTotal()).isEqualByComparingTo("8");
        assertThat(item.valorTotal()).isEqualByComparingTo("80.00");
    }

    @Test
    void divergenciaInventario_agregaAjustesAprovadosPorInsumoNoPeriodo() {
        UUID insumo = fixtures.criarInsumo(categoria, kg, "AJ-01", "Insumo ajustado");
        Instant inicio = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant fim = Instant.now().plus(1, ChronoUnit.HOURS);

        fixtures.criarAjusteAprovado(centro, insumo, kg,
                new BigDecimal("10"), Instant.now().minus(1, ChronoUnit.DAYS));
        fixtures.criarAjusteAprovado(centro, insumo, kg,
                new BigDecimal("-3"), Instant.now().minus(2, ChronoUnit.DAYS));
        // fora do período — não soma
        fixtures.criarAjusteAprovado(centro, insumo, kg,
                new BigDecimal("999"), Instant.now().minus(60, ChronoUnit.DAYS));

        var resultado = divergencia.execute(centro, new PeriodoFiltro(inicio, fim), 0, 50);

        assertThat(resultado).hasSize(1);
        var item = resultado.get(0);
        assertThat(item.quantidadeAjustes()).isEqualTo(2);
        assertThat(item.quantidadeDiffPositiva()).isEqualByComparingTo("10");
        assertThat(item.quantidadeDiffNegativa()).isEqualByComparingTo("3");
        assertThat(item.quantidadeDiffLiquida()).isEqualByComparingTo("7");
    }

    private void registraSaida(UUID filial, UUID insumo, BigDecimal quantidade, BigDecimal valorUnitario) {
        UUID lote = fixtures.criarLote(insumo, "L-" + UUID.randomUUID().toString().substring(0, 8),
                LocalDate.now().plusMonths(6), valorUnitario);
        fixtures.criarSaldo(lote, filial, new BigDecimal("1000"));
        UUID mov = fixtures.criarMovimentacao(filial, usuario, "SAIDA_VENDA",
                Instant.now().minus(1, ChronoUnit.DAYS));
        fixtures.criarItemMovimentacao(mov, insumo, lote, kg, quantidade, valorUnitario);
    }

    private static com.nonnas.reporting.domain.CurvaABCItem curvaPorCodigo(
            java.util.List<com.nonnas.reporting.domain.CurvaABCItem> lista, String codigo) {
        return lista.stream()
                .filter(i -> codigo.equals(i.insumoCodigo()))
                .findFirst().orElseThrow();
    }
}
