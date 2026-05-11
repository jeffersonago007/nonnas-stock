package com.nonnas.recipes.application.venda;

import com.nonnas.inventory.application.movimentacao.RegistrarEntradaManualUseCase;
import com.nonnas.inventory.application.movimentacao.RegistrarEntradaMultiItemUseCase;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.recipes.application.ficha.CriarFichaTecnicaUseCase;
import com.nonnas.recipes.application.produto.CriarProdutoVendavelUseCase;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.testsupport.AbstractRecipesIntegrationTest;
import com.nonnas.sharedkernel.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-LOT-06: cobre o preview da baixa por trás de uma venda — ficha mista com
 * insumo RASTREADO (mussarela, controlaValidade=true) e AGREGADOR (sal,
 * controlaValidade=false) na mesma receita. Valida que a renderização tem
 * dados distintos por regime e que o saldo restante é calculado por insumo.
 *
 * <p>O test popula catalog via JDBC nativo — recipes não importa catalog
 * (ArchUnit), apenas as tabelas precisam existir (test-scope Maven dep em
 * catalog encadeia as Flyway migrations).
 */
class PreviewVendaSimuladaIT extends AbstractRecipesIntegrationTest {

    @Autowired private CriarProdutoVendavelUseCase criarProduto;
    @Autowired private CriarFichaTecnicaUseCase criarFicha;
    @Autowired private PreviewVendaSimuladaUseCase preview;
    @Autowired private RegistrarEntradaManualUseCase entradaManual;
    @Autowired private RegistrarEntradaMultiItemUseCase entradaMulti;
    @Autowired private NamedParameterJdbcTemplate jdbc;

    @Test
    void preview_fichaMistaRastreadoEAgregador_devolveLotesComDadosDistintosPorRegime() {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumoMussarela = UUID.randomUUID();   // RASTREADO
        UUID insumoSal = UUID.randomUUID();          // AGREGADOR
        UUID unidadeKg = unidadeSeed("KG");
        UUID unidadeG = unidadeSeed("G");
        UUID categoria = UUID.randomUUID();

        inserirCategoria(categoria, "Genérico");
        inserirInsumo(insumoMussarela, "MUSS-" + tag(insumoMussarela), "Mussarela", categoria, unidadeKg, true);
        inserirInsumo(insumoSal, "SAL-" + tag(insumoSal), "Sal", categoria, unidadeG, false);

        // Mussarela: RASTREADO com 10kg num lote que vence em 2026-06-01.
        entradaManual.execute(new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumoMussarela, null, null, "MZ-2026-04-A",
                null, LocalDate.parse("2026-06-01"), new BigDecimal("40.00"),
                unidadeKg, new BigDecimal("10"), new BigDecimal("10"),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null));

        // Sal: AGREGADOR com 5000g — multi-item com controlaValidade=false roteia ao lote agregador.
        entradaMulti.execute(new RegistrarEntradaMultiItemUseCase.Comando(
                filial, usuario, TipoMovimentacao.ENTRADA_AJUSTE,
                null, null, "carga inicial sal",
                List.of(new RegistrarEntradaMultiItemUseCase.ItemEntrada(
                        insumoSal, null, null, null, null, null,
                        BigDecimal.ZERO, unidadeG,
                        new BigDecimal("5000"), new BigDecimal("5000"),
                        /* controlaValidade */ false))));

        // Produto + ficha: 0.2kg mussarela + 10g sal por pizza.
        ProdutoVendavel pizza = criarProduto.execute(
                new CriarProdutoVendavelUseCase.Comando("PIZZA-T06", "Pizza Teste", "Pizza"));
        criarFicha.execute(new CriarFichaTecnicaUseCase.Comando(
                pizza.id().value(),
                List.of(
                        new CriarFichaTecnicaUseCase.ItemEntrada(insumoMussarela, unidadeKg, new BigDecimal("0.2")),
                        new CriarFichaTecnicaUseCase.ItemEntrada(insumoSal, unidadeG, new BigDecimal("10"))
                )));

        // Preview de 5 pizzas → 1.0kg mussarela (do lote rastreado) + 50g sal (do agregador).
        PreviewVendaSimuladaUseCase.Resposta resp = preview.execute(
                new PreviewVendaSimuladaUseCase.Comando(pizza.id().value(), filial, new BigDecimal("5")));

        assertThat(resp.itens()).hasSize(2);
        assertThat(resp.gerouNegativo()).isFalse();

        var muss = resp.itens().stream()
                .filter(i -> i.insumoId().equals(insumoMussarela))
                .findFirst().orElseThrow();
        assertThat(muss.insumoNome()).isEqualTo("Mussarela");
        assertThat(muss.unidadeBase()).isEqualTo("KG");
        assertThat(muss.controlaValidade()).isTrue();
        assertThat(muss.quantidadeBase()).isEqualByComparingTo("1.0");
        assertThat(muss.saldoRestanteAposBaixa()).isEqualByComparingTo("9");
        assertThat(muss.lotes()).hasSize(1);
        var loteMuss = muss.lotes().get(0);
        assertThat(loteMuss.numero()).isEqualTo("MZ-2026-04-A");
        assertThat(loteMuss.validade()).isEqualTo(LocalDate.parse("2026-06-01"));
        assertThat(loteMuss.quantidade()).isEqualByComparingTo("1.0");

        var sal = resp.itens().stream()
                .filter(i -> i.insumoId().equals(insumoSal))
                .findFirst().orElseThrow();
        assertThat(sal.insumoNome()).isEqualTo("Sal");
        assertThat(sal.unidadeBase()).isEqualTo("G");
        assertThat(sal.controlaValidade()).isFalse();
        assertThat(sal.quantidadeBase()).isEqualByComparingTo("50");
        assertThat(sal.saldoRestanteAposBaixa()).isEqualByComparingTo("4950");
        assertThat(sal.lotes()).hasSize(1);
        var loteSal = sal.lotes().get(0);
        assertThat(loteSal.numero()).isNull();
        assertThat(loteSal.validade()).isNull();
        assertThat(loteSal.quantidade()).isEqualByComparingTo("50");
    }

    @Test
    void preview_quantidadeMaiorQueSaldoRastreado_sinalizaNegativo() {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID unidade = unidadeSeed("UN");
        UUID categoria = UUID.randomUUID();

        inserirCategoria(categoria, "Genérico");
        inserirInsumo(insumo, "SCARSO-" + tag(insumo), "Insumo escasso", categoria, unidade, true);

        // 3 em estoque.
        entradaManual.execute(new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumo, null, null, "L-1",
                null, LocalDate.parse("2026-12-01"), new BigDecimal("1.00"),
                unidade, new BigDecimal("3"), new BigDecimal("3"),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null));

        ProdutoVendavel produto = criarProduto.execute(
                new CriarProdutoVendavelUseCase.Comando("PRD-NEG", "Produto Negativo", "Generico"));
        criarFicha.execute(new CriarFichaTecnicaUseCase.Comando(
                produto.id().value(),
                List.of(new CriarFichaTecnicaUseCase.ItemEntrada(insumo, unidade, new BigDecimal("1")))));

        // Pede 10 — saldo de 3 não cobre.
        PreviewVendaSimuladaUseCase.Resposta resp = preview.execute(
                new PreviewVendaSimuladaUseCase.Comando(produto.id().value(), filial, new BigDecimal("10")));

        assertThat(resp.gerouNegativo()).isTrue();
        assertThat(resp.itens()).hasSize(1);
        assertThat(resp.itens().get(0).saldoRestanteAposBaixa()).isEqualByComparingTo("-7");
    }

    @Test
    void preview_quantidadeZeroOuNegativa_retornaValidationException() {
        ProdutoVendavel produto = criarProduto.execute(
                new CriarProdutoVendavelUseCase.Comando("PRD-V", "Produto Validacao", "Generico"));

        assertThatThrownBy(() -> preview.execute(
                new PreviewVendaSimuladaUseCase.Comando(produto.id().value(), UUID.randomUUID(), BigDecimal.ZERO)))
                .hasMessageContaining("quantidade");
    }

    @Test
    void preview_produtoSemFichaVigente_retornaNotFound() {
        ProdutoVendavel produto = criarProduto.execute(
                new CriarProdutoVendavelUseCase.Comando("PRD-NF", "Sem ficha", "Generico"));

        assertThatThrownBy(() -> preview.execute(
                new PreviewVendaSimuladaUseCase.Comando(produto.id().value(), UUID.randomUUID(), new BigDecimal("1"))))
                .isInstanceOf(NotFoundException.class);
    }

    private void inserirCategoria(UUID id, String nome) {
        jdbc.update("""
                INSERT INTO categorias_insumo (id, nome, ativa, created_at, updated_at)
                VALUES (:id, :nome, TRUE, :now, :now)
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("nome", nome)
                        .addValue("now", java.sql.Timestamp.from(Instant.now())));
    }

    private UUID unidadeSeed(String codigo) {
        return jdbc.queryForObject(
                "SELECT id FROM unidades_medida WHERE codigo = :codigo",
                new MapSqlParameterSource("codigo", codigo),
                UUID.class);
    }

    private static String tag(UUID id) {
        return id.toString().substring(0, 8);
    }

    private void inserirInsumo(UUID id, String codigo, String nome, UUID categoriaId,
                               UUID unidadeBaseId, boolean controlaValidade) {
        jdbc.update("""
                INSERT INTO insumos (id, codigo, nome, categoria_id, unidade_base_id,
                                     controla_lote, controla_validade, ativo, created_at, updated_at)
                VALUES (:id, :codigo, :nome, :catId, :uniId, TRUE, :cv, TRUE, :now, :now)
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("codigo", codigo)
                        .addValue("nome", nome)
                        .addValue("catId", categoriaId)
                        .addValue("uniId", unidadeBaseId)
                        .addValue("cv", controlaValidade)
                        .addValue("now", java.sql.Timestamp.from(Instant.now())));
    }
}
