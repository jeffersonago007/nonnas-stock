package com.nonnas.inventory.application.movimentacao;

import com.nonnas.inventory.application.events.MovimentacaoCriadaEvent;
import com.nonnas.inventory.application.ports.LoteRepository;
import com.nonnas.inventory.application.ports.MovimentacaoRepository;
import com.nonnas.inventory.domain.ItemMovimentacao;
import com.nonnas.inventory.domain.Lote;
import com.nonnas.inventory.domain.Movimentacao;
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
import java.util.List;
import java.util.UUID;

/**
 * Saída manual com FEFO. Pode produzir múltiplos itens (um por lote
 * consumido). Permite saldo negativo: se a soma dos lotes não basta, o
 * último lote é alocado com a quantidade restante (ficará negativo) e
 * {@code gerouNegativo = true} é registrado na movimentação.
 */
@Service
public class RegistrarSaidaManualUseCase {

    private final SelecionarLotesPorFefoService fefo;
    private final LoteRepository loteRepo;
    private final MovimentacaoRepository movRepo;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public RegistrarSaidaManualUseCase(SelecionarLotesPorFefoService fefo,
                                       LoteRepository loteRepo,
                                       MovimentacaoRepository movRepo,
                                       ApplicationEventPublisher eventos,
                                       Clock clock) {
        this.fefo = fefo;
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
        if (cmd.quantidadeBase == null || cmd.quantidadeBase.signum() <= 0) {
            throw new ValidationException("Quantidade base deve ser positiva");
        }

        var resultado = fefo.selecionar(cmd.insumoId, cmd.filialId, cmd.quantidadeBase);
        if (resultado.semLotes()) {
            throw new BusinessRuleException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Sem lote disponível para o insumo na filial — cadastre uma entrada antes");
        }

        List<ItemMovimentacao> itens = new ArrayList<>();
        for (var aloc : resultado.alocacoes()) {
            // Carrega lote para pegar valor unitário
            Lote lote = loteRepo.findById(aloc.loteId())
                    .orElseThrow(() -> new BusinessRuleException(ErrorCode.UNEXPECTED,
                            "Lote inconsistente: " + aloc.loteId()));
            // Para saída manual, quantidade lançada na unidade base = quantidade base
            // (T03 conversão de unidade só aplica em entradas. MVP: simplificado).
            ItemMovimentacao item = ItemMovimentacao.novo(
                    cmd.insumoId, aloc.loteId(), cmd.unidadeLancamentoId,
                    aloc.quantidade(), aloc.quantidade(), lote.valorUnitario());
            itens.add(item);
        }

        Movimentacao mov = Movimentacao.nova(cmd.filialId, cmd.usuarioId, cmd.tipo,
                clock.instant(), cmd.documentoOrigemTipo, cmd.documentoOrigemId, cmd.observacao,
                resultado.gerouNegativo(), itens, clock.instant());

        Movimentacao saved = movRepo.save(mov);
        eventos.publishEvent(new MovimentacaoCriadaEvent(saved));
        return saved;
    }

    public record Comando(
            UUID filialId,
            UUID usuarioId,
            UUID insumoId,
            UUID unidadeLancamentoId,
            BigDecimal quantidadeBase,
            TipoMovimentacao tipo,
            String documentoOrigemTipo,
            UUID documentoOrigemId,
            String observacao
    ) {}
}
