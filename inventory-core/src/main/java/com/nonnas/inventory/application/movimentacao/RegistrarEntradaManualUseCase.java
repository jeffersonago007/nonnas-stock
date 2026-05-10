package com.nonnas.inventory.application.movimentacao;

import com.nonnas.inventory.application.events.MovimentacaoCriadaEvent;
import com.nonnas.inventory.application.ports.LoteRepository;
import com.nonnas.inventory.application.ports.MovimentacaoRepository;
import com.nonnas.inventory.domain.ItemMovimentacao;
import com.nonnas.inventory.domain.Lote;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Registra entrada manual: cria (ou reutiliza) um Lote e gera Movimentação
 * de tipo ENTRADA_AJUSTE / ENTRADA_NF / ENTRADA_CARGA_INICIAL etc.
 *
 * <p>Para o MVP simplificado, sempre cria um novo Lote — dedup por número
 * de lote vem em onda futura quando NF-e (T16+) for ativada.
 */
@Service
public class RegistrarEntradaManualUseCase {

    private final LoteRepository loteRepo;
    private final MovimentacaoRepository movRepo;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public RegistrarEntradaManualUseCase(LoteRepository loteRepo,
                                         MovimentacaoRepository movRepo,
                                         ApplicationEventPublisher eventos,
                                         Clock clock) {
        this.loteRepo = loteRepo;
        this.movRepo = movRepo;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    public Movimentacao execute(Comando cmd) {
        if (cmd.tipo == null || !cmd.tipo.isEntrada()) {
            throw new ValidationException("Tipo deve ser uma entrada");
        }
        if (cmd.quantidadeBase == null || cmd.quantidadeBase.signum() <= 0) {
            throw new ValidationException("Quantidade base deve ser positiva");
        }

        Lote lote = Lote.novoRastreado(cmd.insumoId, cmd.fornecedorId, cmd.notaFiscalId, cmd.numeroLote,
                cmd.dataFabricacao, cmd.dataValidade, cmd.valorUnitario, clock.instant());
        lote = loteRepo.save(lote);

        ItemMovimentacao item = ItemMovimentacao.novo(
                cmd.insumoId, lote.id(), cmd.unidadeLancamentoId,
                cmd.quantidadeLancada, cmd.quantidadeBase, cmd.valorUnitario);

        Movimentacao mov = Movimentacao.nova(cmd.filialId, cmd.usuarioId, cmd.tipo,
                clock.instant(), cmd.documentoOrigemTipo, cmd.documentoOrigemId, cmd.observacao,
                false, List.of(item), clock.instant());

        Movimentacao saved = movRepo.save(mov);
        eventos.publishEvent(new MovimentacaoCriadaEvent(saved));
        return saved;
    }

    /** DTO interno do comando — evita 14 parâmetros no método. */
    public record Comando(
            UUID filialId,
            UUID usuarioId,
            UUID insumoId,
            UUID fornecedorId,
            UUID notaFiscalId,
            String numeroLote,
            LocalDate dataFabricacao,
            LocalDate dataValidade,
            BigDecimal valorUnitario,
            UUID unidadeLancamentoId,
            BigDecimal quantidadeLancada,
            BigDecimal quantidadeBase,
            TipoMovimentacao tipo,
            String documentoOrigemTipo,
            UUID documentoOrigemId,
            String observacao
    ) {}
}
