package com.nonnas.inventory.application.movimentacao;

import com.nonnas.inventory.application.events.MovimentacaoCriadaEvent;
import com.nonnas.inventory.application.ports.LoteRepository;
import com.nonnas.inventory.application.ports.MovimentacaoRepository;
import com.nonnas.inventory.domain.ItemMovimentacao;
import com.nonnas.inventory.domain.Lote;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.SelecionarLotesParaSaidaService;
import com.nonnas.inventory.domain.SelecionarLotesPorFefoService;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Saída atômica com múltiplos insumos — uma movimentação consolidada
 * (ex.: venda registrada que baixa todos os insumos de uma ficha técnica
 * via FEFO, ou transferência multi-insumo em T06).
 *
 * <p>Cada insumo passa pelo {@link SelecionarLotesPorFefoService} e gera
 * um ou mais {@link ItemMovimentacao}. Todos os itens vão para uma única
 * {@link Movimentacao} dentro da mesma transação. Se qualquer insumo
 * falhar (sem lote disponível), tudo rola back — venda atômica.
 */
@Service
public class RegistrarSaidaMultiItemUseCase {

    private final SelecionarLotesParaSaidaService selecionar;
    private final LoteRepository loteRepo;
    private final MovimentacaoRepository movRepo;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public RegistrarSaidaMultiItemUseCase(SelecionarLotesParaSaidaService selecionar,
                                          LoteRepository loteRepo,
                                          MovimentacaoRepository movRepo,
                                          ApplicationEventPublisher eventos,
                                          Clock clock) {
        this.selecionar = selecionar;
        this.loteRepo = loteRepo;
        this.movRepo = movRepo;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    public Movimentacao execute(Comando cmd) {
        if (cmd.tipo == null || !cmd.tipo.isSaida()) {
            throw new ValidationException("Tipo deve ser uma saída");
        }
        if (cmd.itens == null || cmd.itens.isEmpty()) {
            throw new ValidationException("Pelo menos um item é obrigatório");
        }
        var insumosVistos = new HashSet<UUID>();
        for (var item : cmd.itens) {
            if (item.quantidadeBase == null || item.quantidadeBase.signum() <= 0) {
                throw new ValidationException("Quantidade base deve ser positiva");
            }
            if (!insumosVistos.add(item.insumoId)) {
                throw new ValidationException("Insumo duplicado no comando: " + item.insumoId);
            }
        }

        List<ItemMovimentacao> consolidados = new ArrayList<>();
        boolean gerouNegativo = false;

        for (var item : cmd.itens) {
            var resultado = selecionar.selecionar(item.insumoId, cmd.filialId, item.quantidadeBase);
            if (resultado.semLotes()) {
                throw new BusinessRuleException(ErrorCode.BUSINESS_RULE_VIOLATED,
                        "Sem lote disponível para o insumo " + item.insumoId
                                + " na filial — cadastre uma entrada antes");
            }
            gerouNegativo = gerouNegativo || resultado.gerouNegativo();
            for (var aloc : resultado.alocacoes()) {
                Lote lote = loteRepo.findById(aloc.loteId())
                        .orElseThrow(() -> new BusinessRuleException(ErrorCode.UNEXPECTED,
                                "Lote inconsistente: " + aloc.loteId()));
                ItemMovimentacao mi = ItemMovimentacao.novo(
                        item.insumoId, aloc.loteId(), item.unidadeLancamentoId,
                        aloc.quantidade(), aloc.quantidade(), lote.valorUnitario());
                consolidados.add(mi);
            }
        }

        Movimentacao mov = Movimentacao.nova(cmd.filialId, cmd.usuarioId, cmd.tipo,
                clock.instant(), cmd.documentoOrigemTipo, cmd.documentoOrigemId, cmd.observacao,
                gerouNegativo, consolidados, clock.instant());

        Movimentacao saved = movRepo.save(mov);
        eventos.publishEvent(new MovimentacaoCriadaEvent(saved));
        return saved;
    }

    public record Comando(
            UUID filialId,
            UUID usuarioId,
            TipoMovimentacao tipo,
            String documentoOrigemTipo,
            UUID documentoOrigemId,
            String observacao,
            List<ItemSaida> itens
    ) {}

    public record ItemSaida(
            UUID insumoId,
            UUID unidadeLancamentoId,
            BigDecimal quantidadeBase
    ) {}
}
