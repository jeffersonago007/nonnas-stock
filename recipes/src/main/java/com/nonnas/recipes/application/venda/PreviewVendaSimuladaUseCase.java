package com.nonnas.recipes.application.venda;

import com.nonnas.inventory.domain.SelecionarLotesParaSaidaService;
import com.nonnas.inventory.domain.SelecionarLotesPorFefoService;
import com.nonnas.recipes.application.ports.FichaTecnicaRepository;
import com.nonnas.recipes.application.ports.PreviewVendaQueries;
import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.domain.FichaTecnica;
import com.nonnas.recipes.domain.ItemFichaTecnica;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.recipes.domain.TipoProdutoVendavel;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Preview da baixa por trás de uma venda. Bifurca por tipo do produto:
 * <ul>
 *   <li>{@code FABRICADO}: expande a ficha técnica e simula FEFO/AGREGADOR
 *       por insumo, sem persistir.</li>
 *   <li>{@code REVENDA}: simula a baixa 1:1 do insumo vinculado.</li>
 * </ul>
 * Em ambos os casos, enriquece com metadados de catalog (nome, unidade,
 * controla_validade) e inventory (lote número/validade) via JDBC nativo.
 */
@Service
public class PreviewVendaSimuladaUseCase {

    private final ProdutoVendavelRepository produtoRepo;
    private final FichaTecnicaRepository fichaRepo;
    private final SelecionarLotesParaSaidaService selecionar;
    private final PreviewVendaQueries queries;

    public PreviewVendaSimuladaUseCase(ProdutoVendavelRepository produtoRepo,
                                       FichaTecnicaRepository fichaRepo,
                                       SelecionarLotesParaSaidaService selecionar,
                                       PreviewVendaQueries queries) {
        this.produtoRepo = produtoRepo;
        this.fichaRepo = fichaRepo;
        this.selecionar = selecionar;
        this.queries = queries;
    }

    // SelecionarLotesParaSaidaService usa PESSIMISTIC_WRITE no FEFO; transação
    // não-readOnly permite o lock ser adquirido (e liberado no commit sem write).
    @Transactional
    public Resposta execute(Comando cmd) {
        if (cmd.quantidadeVendida == null || cmd.quantidadeVendida.signum() <= 0) {
            throw new ValidationException("Informe uma quantidade vendida maior que zero");
        }

        ProdutoVendavel produto = produtoRepo.findById(ProdutoVendavelId.of(cmd.produtoVendavelId))
                .orElseThrow(() -> new NotFoundException("Produto vendável", cmd.produtoVendavelId));

        List<InsumoNecessario> necessarios = produto.tipo() == TipoProdutoVendavel.REVENDA
                ? necessariosRevenda(produto, cmd)
                : necessariosFabricado(produto, cmd);

        // 1) Simula a seleção FEFO/AGREGADOR para cada insumo, sem persistir.
        List<SelecaoPorItem> selecoes = new ArrayList<>();
        boolean algumNegativo = false;
        for (InsumoNecessario n : necessarios) {
            SelecionarLotesPorFefoService.Resultado res = selecionar.selecionar(
                    n.insumoId(), cmd.filialId, n.quantidadeBase());
            selecoes.add(new SelecaoPorItem(n.insumoId(), n.quantidadeBase(), res));
            algumNegativo = algumNegativo || res.gerouNegativo();
        }

        // 2) Bulk fetch de metadados cross-context.
        Set<UUID> insumoIds = new LinkedHashSet<>();
        Set<UUID> loteIds = new LinkedHashSet<>();
        for (SelecaoPorItem s : selecoes) {
            insumoIds.add(s.insumoId);
            for (var aloc : s.resultado.alocacoes()) {
                loteIds.add(aloc.loteId().value());
            }
        }
        Map<UUID, PreviewVendaQueries.InsumoMeta> insumoMeta = queries.fetchInsumoMeta(insumoIds, cmd.filialId);
        Map<UUID, PreviewVendaQueries.LoteMeta> loteMeta = queries.fetchLoteMeta(loteIds);

        // 3) Compõe a resposta agrupada por insumo.
        List<ItemBaixaPreview> itens = new ArrayList<>();
        for (SelecaoPorItem s : selecoes) {
            PreviewVendaQueries.InsumoMeta meta = insumoMeta.get(s.insumoId);
            if (meta == null) {
                throw new NotFoundException("Insumo " + s.insumoId + " não está em catalog");
            }
            BigDecimal saldoRestante = meta.saldoAtual().subtract(s.quantidadeBase);

            List<LoteConsumido> lotes = new ArrayList<>();
            for (var aloc : s.resultado.alocacoes()) {
                UUID loteId = aloc.loteId().value();
                PreviewVendaQueries.LoteMeta lm = loteMeta.get(loteId);
                String numero = lm == null ? null : lm.numeroLote();
                LocalDate validade = lm == null ? null : lm.dataValidade();
                lotes.add(new LoteConsumido(loteId, numero, validade, aloc.quantidade()));
            }

            itens.add(new ItemBaixaPreview(
                    s.insumoId,
                    meta.nome(),
                    s.quantidadeBase,
                    meta.unidadeBaseCodigo(),
                    meta.controlaValidade(),
                    List.copyOf(lotes),
                    saldoRestante
            ));
        }

        return new Resposta(List.copyOf(itens), algumNegativo);
    }

    private List<InsumoNecessario> necessariosFabricado(ProdutoVendavel produto, Comando cmd) {
        FichaTecnica vigente = fichaRepo.findVigentePorProduto(produto.id())
                .orElseThrow(() -> new NotFoundException(
                        "Ficha técnica vigente para o produto " + produto.id().value() + " não encontrada"));
        return vigente.itens().stream()
                .map((ItemFichaTecnica i) -> new InsumoNecessario(
                        i.insumoId(),
                        i.quantidade().multiply(cmd.quantidadeVendida)))
                .toList();
    }

    private List<InsumoNecessario> necessariosRevenda(ProdutoVendavel produto, Comando cmd) {
        UUID insumoId = produto.insumoRevendaIdOpt().orElseThrow(() ->
                new ValidationException("Produto de revenda sem insumo vinculado (estado inconsistente)"));
        return List.of(new InsumoNecessario(insumoId, cmd.quantidadeVendida));
    }

    private record InsumoNecessario(UUID insumoId, BigDecimal quantidadeBase) {}

    private record SelecaoPorItem(UUID insumoId, BigDecimal quantidadeBase,
                                  SelecionarLotesPorFefoService.Resultado resultado) {}

    public record Comando(UUID produtoVendavelId, UUID filialId, BigDecimal quantidadeVendida) {}

    public record Resposta(List<ItemBaixaPreview> itens, boolean gerouNegativo) {}

    public record ItemBaixaPreview(
            UUID insumoId,
            String insumoNome,
            BigDecimal quantidadeBase,
            String unidadeBase,
            boolean controlaValidade,
            List<LoteConsumido> lotes,
            BigDecimal saldoRestanteAposBaixa
    ) {}

    public record LoteConsumido(
            UUID loteId,
            String numero,
            LocalDate validade,
            BigDecimal quantidade
    ) {}
}
