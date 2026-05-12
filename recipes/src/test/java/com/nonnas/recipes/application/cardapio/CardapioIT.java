package com.nonnas.recipes.application.cardapio;

import com.nonnas.inventory.application.movimentacao.RegistrarEntradaManualUseCase;
import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.application.produto.CriarProdutoVendavelUseCase;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.recipes.domain.TipoProdutoVendavel;
import com.nonnas.recipes.testsupport.AbstractRecipesIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o cardápio unificado e a auto-promoção de insumo órfão a produto
 * REVENDA na primeira venda.
 */
class CardapioIT extends AbstractRecipesIntegrationTest {

    @Autowired private ListarCardapioUseCase listar;
    @Autowired private VenderInsumoOrfaoUseCase vender;
    @Autowired private CriarProdutoVendavelUseCase criarProduto;
    @Autowired private RegistrarEntradaManualUseCase entrada;
    @Autowired private ProdutoVendavelRepository produtoRepo;
    @Autowired private SaldoLoteRepository saldoRepo;
    @Autowired private NamedParameterJdbcTemplate jdbc;

    @Test
    void cardapio_listaProdutosEInsumosOrfaosComSaldo() {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID unidade = unidadeSeed("UN");
        UUID categoria = UUID.randomUUID();
        inserirCategoria(categoria, "Diversos");

        UUID insumoOrfao = UUID.randomUUID();
        UUID insumoVinculado = UUID.randomUUID();
        UUID insumoSemSaldo = UUID.randomUUID();
        inserirInsumo(insumoOrfao, "ORF-" + tag(insumoOrfao), "Item Órfão", categoria, unidade);
        inserirInsumo(insumoVinculado, "VIN-" + tag(insumoVinculado), "Item Vinculado", categoria, unidade);
        inserirInsumo(insumoSemSaldo, "ZERO-" + tag(insumoSemSaldo), "Item Sem Saldo", categoria, unidade);

        // Saldo só pros dois primeiros.
        criarLote(filial, usuario, insumoOrfao, unidade, "L-1", 10);
        criarLote(filial, usuario, insumoVinculado, unidade, "L-2", 5);

        // Cria produto FABRICADO + produto REVENDA do insumoVinculado.
        criarProduto.execute(new CriarProdutoVendavelUseCase.Comando(
                "PIZ-1", "Pizza Teste", "Pizza"));
        criarProduto.execute(new CriarProdutoVendavelUseCase.Comando(
                "REV-1", "Item Vinculado Revenda", "Diversos",
                TipoProdutoVendavel.REVENDA, insumoVinculado));

        ListarCardapioUseCase.Resposta resp = listar.execute(filial);

        // Filtro só pelos itens deste teste — outros testes na mesma classe
        // podem ter populado dados que continuam visíveis.
        var idsDoTeste = java.util.Set.of(insumoOrfao, insumoVinculado, insumoSemSaldo);
        var idsDoTesteOuProdutosCodigosDoTeste = resp.itens().stream()
                .filter(i -> i.codigo().startsWith("PIZ-1")
                          || i.codigo().equals("REV-1")
                          || idsDoTeste.contains(i.id()))
                .toList();

        assertThat(idsDoTesteOuProdutosCodigosDoTeste).hasSize(3);

        assertThat(idsDoTesteOuProdutosCodigosDoTeste).anyMatch(i ->
                i.origem() == ListarCardapioUseCase.Origem.PRODUTO_FABRICADO
                && i.nome().equals("PIZZA TESTE"));
        assertThat(idsDoTesteOuProdutosCodigosDoTeste).anyMatch(i ->
                i.origem() == ListarCardapioUseCase.Origem.PRODUTO_REVENDA
                && i.codigo().equals("REV-1"));

        var orfaos = idsDoTesteOuProdutosCodigosDoTeste.stream()
                .filter(i -> i.origem() == ListarCardapioUseCase.Origem.INSUMO_ORFAO).toList();
        assertThat(orfaos).hasSize(1);
        assertThat(orfaos.get(0).id()).isEqualTo(insumoOrfao);
        assertThat(orfaos.get(0).saldoNaFilial()).isEqualByComparingTo("10");
        assertThat(orfaos.get(0).unidadeBaseCodigo()).isEqualTo("UN");
    }

    @Test
    void venderInsumo_orfao_autoPromoveARevendaEBaixaSaldo() {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID unidade = unidadeSeed("UN");
        UUID categoria = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        inserirCategoria(categoria, "Bebidas");
        inserirInsumo(insumo, "COCA-" + tag(insumo), "Coca-Cola 2L", categoria, unidade);
        criarLote(filial, usuario, insumo, unidade, "COCA-LT-1", 20);

        VenderInsumoOrfaoUseCase.Resposta resp = vender.execute(new VenderInsumoOrfaoUseCase.Comando(
                insumo, filial, usuario, new BigDecimal("3"), "primeira venda da coca"));

        // 1) Movimentação registrada como venda REVENDA do produto recém-criado.
        assertThat(resp.movimentacao().tipo()).isEqualTo(TipoMovimentacao.SAIDA_VENDA);
        assertThat(resp.movimentacao().documentoOrigemTipoOpt()).contains("PRODUTO_REVENDA");
        assertThat(resp.movimentacao().documentoOrigemIdOpt()).contains(resp.produtoVendavelCriadoId());

        // 2) Produto REVENDA criado com codigo/nome do insumo + categoria default.
        ProdutoVendavel criado = produtoRepo.findById(
                ProdutoVendavelId.of(resp.produtoVendavelCriadoId())).orElseThrow();
        assertThat(criado.tipo()).isEqualTo(TipoProdutoVendavel.REVENDA);
        assertThat(criado.codigo()).startsWith("COCA-");
        assertThat(criado.nome()).isEqualTo("COCA-COLA 2L");
        assertThat(criado.categoria()).isEqualTo("A classificar");
        assertThat(criado.insumoRevendaIdOpt()).contains(insumo);

        // 3) Saldo do insumo baixou 3 unidades.
        assertThat(saldoRepo.somarPorInsumoEFilial(insumo, filial))
                .isEqualByComparingTo("17");

        // 4) Na próxima listagem, o insumo deixa de ser órfão (já tem produto vinculado).
        ListarCardapioUseCase.Resposta cardapio = listar.execute(filial);
        var orfaos = cardapio.itens().stream()
                .filter(i -> i.origem() == ListarCardapioUseCase.Origem.INSUMO_ORFAO).toList();
        assertThat(orfaos.stream().map(ListarCardapioUseCase.ItemCardapio::id))
                .doesNotContain(insumo);
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

    private void inserirInsumo(UUID id, String codigo, String nome, UUID categoriaId, UUID unidadeBaseId) {
        jdbc.update("""
                INSERT INTO insumos (id, codigo, nome, categoria_id, unidade_base_id,
                                     controla_lote, controla_validade, ativo, created_at, updated_at)
                VALUES (:id, :codigo, :nome, :catId, :uniId, TRUE, FALSE, TRUE, :now, :now)
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("codigo", codigo)
                        .addValue("nome", nome)
                        .addValue("catId", categoriaId)
                        .addValue("uniId", unidadeBaseId)
                        .addValue("now", java.sql.Timestamp.from(Instant.now())));
    }

    private void criarLote(UUID filial, UUID usuario, UUID insumo, UUID unidade, String numero, int qtd) {
        entrada.execute(new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumo, null, null, numero,
                null, LocalDate.parse("2026-12-01"), new BigDecimal("10.00"),
                unidade, new BigDecimal(qtd), new BigDecimal(qtd),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null));
    }
}
