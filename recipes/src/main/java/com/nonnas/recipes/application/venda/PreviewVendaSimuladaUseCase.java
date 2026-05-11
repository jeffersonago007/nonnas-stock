package com.nonnas.recipes.application.venda;

import com.nonnas.inventory.domain.SelecionarLotesParaSaidaService;
import com.nonnas.inventory.domain.SelecionarLotesPorFefoService;
import com.nonnas.recipes.application.ports.FichaTecnicaRepository;
import com.nonnas.recipes.application.ports.PreviewVendaQueries;
import com.nonnas.recipes.domain.FichaTecnica;
import com.nonnas.recipes.domain.ItemFichaTecnica;
import com.nonnas.recipes.domain.ProdutoVendavelId;
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
 * Preview da baixa por trás de uma venda: resolve a ficha vigente, simula a
 * seleção de lotes (FEFO ou AGREGADOR conforme o regime de cada insumo) sem
 * persistir nada e enriquece com metadados de catalog (nome, unidade,
 * controla_validade) e inventory (lote número/validade) via JDBC nativo.
 *
 * <p>T-LOT-06: a renderização no frontend é condicional ao {@code controlaValidade}
 * — RASTREADO mostra lote+validade, AGREGADOR mostra só saldo após a baixa.
 */
@Service
public class PreviewVendaSimuladaUseCase {

    private final FichaTecnicaRepository fichaRepo;
    private final SelecionarLotesParaSaidaService selecionar;
    private final PreviewVendaQueries queries;

    public PreviewVendaSimuladaUseCase(FichaTecnicaRepository fichaRepo,
                                       SelecionarLotesParaSaidaService selecionar,
                                       PreviewVendaQueries queries) {
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

        FichaTecnica vigente = fichaRepo.findVigentePorProduto(ProdutoVendavelId.of(cmd.produtoVendavelId))
                .orElseThrow(() -> new NotFoundException(
                        "Ficha técnica vigente para o produto " + cmd.produtoVendavelId + " não encontrada"));

        // 1) Simula a seleção FEFO/AGREGADOR para cada item da ficha, sem persistir.
        List<SelecaoPorItem> selecoes = new ArrayList<>();
        boolean algumNegativo = false;
        for (ItemFichaTecnica item : vigente.itens()) {
            BigDecimal qBase = item.quantidade().multiply(cmd.quantidadeVendida);
            SelecionarLotesPorFefoService.Resultado res = selecionar.selecionar(
                    item.insumoId(), cmd.filialId, qBase);
            selecoes.add(new SelecaoPorItem(item.insumoId(), qBase, res));
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
                throw new NotFoundException("Insumo " + s.insumoId + " referenciado na ficha não está em catalog");
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
