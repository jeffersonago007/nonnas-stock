package com.nonnas.recipes.application.venda;

import com.nonnas.inventory.application.movimentacao.RegistrarSaidaMultiItemUseCase;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.recipes.application.ports.CatalogQueries;
import com.nonnas.recipes.application.ports.FichaTecnicaRepository;
import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.domain.FichaTecnica;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.recipes.domain.TipoProdutoVendavel;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Registra a baixa de estoque por trás de uma venda. Bifurca por tipo do produto:
 * <ul>
 *   <li>{@code FABRICADO}: resolve ficha técnica vigente e gera uma movimentação
 *       SAIDA_VENDA multi-item via FEFO. {@code documento_origem_tipo='FICHA_TECNICA'}.</li>
 *   <li>{@code REVENDA}: baixa 1:1 do insumo vinculado, sem ficha técnica.
 *       {@code documento_origem_tipo='PRODUTO_REVENDA'}.</li>
 * </ul>
 */
@Service
public class RegistrarVendaSimuladaUseCase {

    private static final String DOC_ORIGEM_FICHA = "FICHA_TECNICA";
    private static final String DOC_ORIGEM_REVENDA = "PRODUTO_REVENDA";

    private final ProdutoVendavelRepository produtoRepo;
    private final FichaTecnicaRepository fichaRepo;
    private final CatalogQueries catalog;
    private final RegistrarSaidaMultiItemUseCase saidaMulti;

    public RegistrarVendaSimuladaUseCase(ProdutoVendavelRepository produtoRepo,
                                         FichaTecnicaRepository fichaRepo,
                                         CatalogQueries catalog,
                                         RegistrarSaidaMultiItemUseCase saidaMulti) {
        this.produtoRepo = produtoRepo;
        this.fichaRepo = fichaRepo;
        this.catalog = catalog;
        this.saidaMulti = saidaMulti;
    }

    @Transactional
    public Movimentacao execute(Comando cmd) {
        if (cmd.quantidadeVendida == null || cmd.quantidadeVendida.signum() <= 0) {
            throw new ValidationException("Quantidade vendida deve ser positiva");
        }

        ProdutoVendavelId produtoId = ProdutoVendavelId.of(cmd.produtoVendavelId);
        ProdutoVendavel produto = produtoRepo.findById(produtoId)
                .orElseThrow(() -> new NotFoundException("Produto vendável", cmd.produtoVendavelId));

        return produto.tipo() == TipoProdutoVendavel.REVENDA
                ? vendaRevenda(produto, cmd)
                : vendaFabricado(produto, cmd);
    }

    private Movimentacao vendaFabricado(ProdutoVendavel produto, Comando cmd) {
        FichaTecnica vigente = fichaRepo.findVigentePorProduto(produto.id())
                .orElseThrow(() -> new NotFoundException(
                        "Ficha técnica vigente para o produto " + produto.id().value() + " não encontrada"));

        List<RegistrarSaidaMultiItemUseCase.ItemSaida> itens = vigente.itens().stream()
                .map(i -> new RegistrarSaidaMultiItemUseCase.ItemSaida(
                        i.insumoId(),
                        i.unidadeId(),
                        i.quantidade().multiply(cmd.quantidadeVendida)))
                .toList();

        var saidaCmd = new RegistrarSaidaMultiItemUseCase.Comando(
                cmd.filialId, cmd.usuarioId, TipoMovimentacao.SAIDA_VENDA,
                DOC_ORIGEM_FICHA, vigente.id().value(),
                cmd.observacao, itens);
        return saidaMulti.execute(saidaCmd);
    }

    private Movimentacao vendaRevenda(ProdutoVendavel produto, Comando cmd) {
        UUID insumoId = produto.insumoRevendaIdOpt().orElseThrow(() ->
                new ValidationException("Produto de revenda sem insumo vinculado (estado inconsistente)"));
        UUID unidadeId = catalog.findUnidadeBaseDoInsumo(insumoId)
                .orElseThrow(() -> new NotFoundException("Insumo vinculado", insumoId));

        var item = new RegistrarSaidaMultiItemUseCase.ItemSaida(
                insumoId, unidadeId, cmd.quantidadeVendida);

        var saidaCmd = new RegistrarSaidaMultiItemUseCase.Comando(
                cmd.filialId, cmd.usuarioId, TipoMovimentacao.SAIDA_VENDA,
                DOC_ORIGEM_REVENDA, produto.id().value(),
                cmd.observacao, List.of(item));
        return saidaMulti.execute(saidaCmd);
    }

    public record Comando(
            UUID produtoVendavelId,
            UUID filialId,
            UUID usuarioId,
            BigDecimal quantidadeVendida,
            String observacao
    ) {}
}
