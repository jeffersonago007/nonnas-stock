package com.nonnas.recipes.application.venda;

import com.nonnas.inventory.application.movimentacao.RegistrarEntradaManualUseCase;
import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.recipes.application.ficha.CriarFichaTecnicaUseCase;
import com.nonnas.recipes.application.produto.CriarProdutoVendavelUseCase;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.TipoProdutoVendavel;
import com.nonnas.recipes.testsupport.AbstractRecipesIntegrationTest;
import com.nonnas.sharedkernel.BusinessRuleException;
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
 * Cobre o modelo FABRICADO vs REVENDA (ADR 0015):
 * 1. REVENDA + venda baixa 1:1 do insumo vinculado, sem ficha técnica;
 * 2. Tentar criar ficha técnica em produto REVENDA é bloqueado;
 * 3. Tentar criar produto REVENDA com insumo inexistente é bloqueado.
 */
class VendaRevendaIT extends AbstractRecipesIntegrationTest {

    @Autowired private CriarProdutoVendavelUseCase criarProduto;
    @Autowired private CriarFichaTecnicaUseCase criarFicha;
    @Autowired private RegistrarVendaSimuladaUseCase venda;
    @Autowired private RegistrarEntradaManualUseCase entrada;
    @Autowired private SaldoLoteRepository saldoRepo;
    @Autowired private NamedParameterJdbcTemplate jdbc;

    @Test
    void venda_revenda_baixaUnaUnidadeDoInsumoVinculado_semFichaTecnica() {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumoCoca = UUID.randomUUID();
        UUID unidadeUn = unidadeSeed("UN");
        UUID categoria = UUID.randomUUID();

        inserirCategoria(categoria, "Bebidas");
        inserirInsumo(insumoCoca, "COCA-2L-" + tag(insumoCoca), "Coca-Cola 2L", categoria, unidadeUn, false);

        // 20 garrafas em estoque (RASTREADO mesmo — flag controla_validade=false no insumo
        // só importa pro fluxo da NF-e; aqui criamos lote diretamente).
        entrada.execute(new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumoCoca, null, null, "COCA-LOTE-1",
                null, LocalDate.parse("2026-12-01"), new BigDecimal("8.50"),
                unidadeUn, new BigDecimal("20"), new BigDecimal("20"),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null));

        ProdutoVendavel coca = criarProduto.execute(new CriarProdutoVendavelUseCase.Comando(
                "PROD-COCA", "Coca-Cola 2L", "Bebidas",
                TipoProdutoVendavel.REVENDA, insumoCoca));

        assertThat(coca.tipo()).isEqualTo(TipoProdutoVendavel.REVENDA);
        assertThat(coca.insumoRevendaIdOpt()).contains(insumoCoca);

        Movimentacao mov = venda.execute(new RegistrarVendaSimuladaUseCase.Comando(
                coca.id().value(), filial, usuario, new BigDecimal("3"), "venda revenda"));

        assertThat(mov.tipo()).isEqualTo(TipoMovimentacao.SAIDA_VENDA);
        assertThat(mov.documentoOrigemTipoOpt()).contains("PRODUTO_REVENDA");
        assertThat(mov.documentoOrigemIdOpt()).contains(coca.id().value());
        assertThat(mov.itens()).hasSize(1);
        assertThat(mov.itens().get(0).quantidadeBase()).isEqualByComparingTo("3");
        assertThat(saldoRepo.somarPorInsumoEFilial(insumoCoca, filial))
                .isEqualByComparingTo("17");
    }

    @Test
    void criarFicha_emProdutoRevenda_retornaBusinessRule() {
        UUID insumo = UUID.randomUUID();
        UUID unidade = unidadeSeed("UN");
        UUID categoria = UUID.randomUUID();
        inserirCategoria(categoria, "Bebidas");
        inserirInsumo(insumo, "AGUA-" + tag(insumo), "Água Mineral 500ml", categoria, unidade, false);

        ProdutoVendavel agua = criarProduto.execute(new CriarProdutoVendavelUseCase.Comando(
                "PROD-AGUA", "Água Mineral", "Bebidas",
                TipoProdutoVendavel.REVENDA, insumo));

        assertThatThrownBy(() -> criarFicha.execute(new CriarFichaTecnicaUseCase.Comando(
                agua.id().value(),
                List.of(new CriarFichaTecnicaUseCase.ItemEntrada(insumo, unidade, BigDecimal.ONE)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("revenda");
    }

    @Test
    void criarRevenda_comInsumoInexistente_retornaNotFound() {
        UUID inexistente = UUID.randomUUID();
        assertThatThrownBy(() -> criarProduto.execute(new CriarProdutoVendavelUseCase.Comando(
                "PROD-FANTASMA", "Produto Fantasma", "Bebidas",
                TipoProdutoVendavel.REVENDA, inexistente)))
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
