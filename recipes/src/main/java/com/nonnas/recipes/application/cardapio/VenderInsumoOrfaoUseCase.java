package com.nonnas.recipes.application.cardapio;

import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.recipes.application.ports.CatalogQueries;
import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.application.produto.CriarProdutoVendavelUseCase;
import com.nonnas.recipes.application.venda.RegistrarVendaSimuladaUseCase;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.TipoProdutoVendavel;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

/**
 * Vende um insumo "órfão" (que ainda não tem produto vendável vinculado)
 * promovendo-o silenciosamente a {@code ProdutoVendavel} do tipo REVENDA.
 *
 * <p>Próximas vendas daquele insumo encontram o produto já cadastrado e
 * caem no fluxo normal de {@link RegistrarVendaSimuladaUseCase}. O nome e
 * código são copiados do insumo; categoria padrão {@code "A classificar"}
 * — o operador pode editar depois em {@code /cardápio}.
 */
@Service
public class VenderInsumoOrfaoUseCase {

    private static final String CATEGORIA_DEFAULT = "A classificar";

    private final CatalogQueries catalog;
    private final ProdutoVendavelRepository produtoRepo;
    private final CriarProdutoVendavelUseCase criarProduto;
    private final RegistrarVendaSimuladaUseCase venda;
    private final Clock clock;

    public VenderInsumoOrfaoUseCase(CatalogQueries catalog,
                                    ProdutoVendavelRepository produtoRepo,
                                    CriarProdutoVendavelUseCase criarProduto,
                                    RegistrarVendaSimuladaUseCase venda,
                                    Clock clock) {
        this.catalog = catalog;
        this.produtoRepo = produtoRepo;
        this.criarProduto = criarProduto;
        this.venda = venda;
        this.clock = clock;
    }

    @Transactional
    public Resposta execute(Comando cmd) {
        var insumo = catalog.findInsumoComSaldo(cmd.insumoId, cmd.filialId)
                .orElseThrow(() -> new NotFoundException("Insumo", cmd.insumoId));

        // Se já existe produto REVENDA vinculado a esse insumo (mesmo inativo),
        // reativa em vez de criar duplicata — preserva código/nome/categoria
        // que o operador eventualmente customizou.
        ProdutoVendavel produto = produtoRepo.findRevendaPorInsumo(insumo.insumoId())
                .map(existente -> {
                    if (!existente.ativo()) {
                        existente.ativar(clock.instant());
                        return produtoRepo.save(existente);
                    }
                    return existente;
                })
                .orElseGet(() -> criarProduto.execute(new CriarProdutoVendavelUseCase.Comando(
                        insumo.codigo(), insumo.nome(), CATEGORIA_DEFAULT,
                        TipoProdutoVendavel.REVENDA, insumo.insumoId())));

        Movimentacao mov = venda.execute(new RegistrarVendaSimuladaUseCase.Comando(
                produto.id().value(), cmd.filialId, cmd.usuarioId,
                cmd.quantidadeVendida, cmd.observacao));

        return new Resposta(produto.id().value(), mov);
    }

    public record Comando(
            UUID insumoId,
            UUID filialId,
            UUID usuarioId,
            BigDecimal quantidadeVendida,
            String observacao
    ) {}

    public record Resposta(UUID produtoVendavelCriadoId, Movimentacao movimentacao) {}
}
