package com.nonnas.recipes.application.cardapio;

import com.nonnas.recipes.application.ports.CatalogQueries;
import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.domain.ProdutoVendavel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Devolve a lista unificada da tela Vendas: produtos vendáveis ativos +
 * insumos órfãos com saldo > 0 na filial (insumos que ainda não foram
 * promovidos a produto REVENDA). Operador vê tudo num só lugar.
 */
@Service
public class ListarCardapioUseCase {

    private final ProdutoVendavelRepository produtoRepo;
    private final CatalogQueries catalog;

    public ListarCardapioUseCase(ProdutoVendavelRepository produtoRepo, CatalogQueries catalog) {
        this.produtoRepo = produtoRepo;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public Resposta execute(UUID filialId) {
        // findFiltered devolve lista imutável (Page.getContent); copiamos pra ordenar.
        List<ProdutoVendavel> produtos = new ArrayList<>(
                produtoRepo.findFiltered(null, true, null, null, 0, 500));
        var vinculados = produtoRepo.listarInsumosVinculadosARevenda();
        List<CatalogQueries.InsumoComSaldo> orfaos = new ArrayList<>(
                catalog.findInsumosOrfaosComSaldo(filialId, vinculados));
        Map<UUID, Integer> ranking = catalog.contarVendasPorProdutoUltimos30Dias(filialId);

        // Ordena produtos: mais vendidos primeiro (30d), depois alfabético.
        // Insumos órfãos vão no fim, alfabéticos.
        Collator ptBR = Collator.getInstance(Locale.of("pt", "BR"));
        ptBR.setStrength(Collator.PRIMARY);

        produtos.sort(Comparator
                .comparing((ProdutoVendavel p) -> ranking.getOrDefault(p.id().value(), 0)).reversed()
                .thenComparing(ProdutoVendavel::nome, ptBR));

        orfaos.sort(Comparator.comparing(CatalogQueries.InsumoComSaldo::nome, ptBR));

        List<ItemCardapio> itens = new ArrayList<>(produtos.size() + orfaos.size());
        for (ProdutoVendavel p : produtos) {
            itens.add(ItemCardapio.deProduto(p, ranking.getOrDefault(p.id().value(), 0)));
        }
        for (var o : orfaos) {
            itens.add(ItemCardapio.deInsumoOrfao(o));
        }
        return new Resposta(itens);
    }

    public record Resposta(List<ItemCardapio> itens) {}

    public record ItemCardapio(
            Origem origem,
            UUID id,                       // produtoVendavelId OU insumoId conforme origem
            String codigo,
            String nome,
            String categoria,              // null para insumo órfão
            String unidadeBaseCodigo,      // null para FABRICADO (vem da ficha); preenchido pros outros
            BigDecimal saldoNaFilial,      // null para FABRICADO; preenchido pros outros
            int vendasUltimos30Dias        // 0 para insumo órfão e produtos sem venda recente
    ) {
        static ItemCardapio deProduto(ProdutoVendavel p, int vendas) {
            return new ItemCardapio(
                    p.tipo() == com.nonnas.recipes.domain.TipoProdutoVendavel.REVENDA
                            ? Origem.PRODUTO_REVENDA
                            : Origem.PRODUTO_FABRICADO,
                    p.id().value(),
                    p.codigo(),
                    p.nome(),
                    p.categoria(),
                    null,
                    null,
                    vendas);
        }

        static ItemCardapio deInsumoOrfao(CatalogQueries.InsumoComSaldo o) {
            return new ItemCardapio(
                    Origem.INSUMO_ORFAO,
                    o.insumoId(),
                    o.codigo(),
                    o.nome(),
                    null,
                    o.unidadeBaseCodigo(),
                    o.saldo(),
                    0);
        }
    }

    public enum Origem { PRODUTO_FABRICADO, PRODUTO_REVENDA, INSUMO_ORFAO }
}
