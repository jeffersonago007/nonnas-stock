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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entrada atômica de múltiplos insumos — uma única movimentação consolidada
 * com N lotes novos (ex.: recebimento de transferência multi-insumo, carga
 * inicial multi-linha como uma operação única). Espelha
 * {@link RegistrarSaidaMultiItemUseCase}.
 *
 * <p>Para cada item, cria um {@link Lote} novo e o respectivo
 * {@link ItemMovimentacao}. Tudo dentro de uma transação — falha em qualquer
 * item rola back todos os lotes e a movimentação.
 */
@Service
public class RegistrarEntradaMultiItemUseCase {

    private final LoteRepository loteRepo;
    private final MovimentacaoRepository movRepo;
    private final BuscarOuCriarLoteAgregadorUseCase agregador;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public RegistrarEntradaMultiItemUseCase(LoteRepository loteRepo,
                                            MovimentacaoRepository movRepo,
                                            BuscarOuCriarLoteAgregadorUseCase agregador,
                                            ApplicationEventPublisher eventos,
                                            Clock clock) {
        this.loteRepo = loteRepo;
        this.movRepo = movRepo;
        this.agregador = agregador;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    public Movimentacao execute(Comando cmd) {
        if (cmd.tipo == null || !cmd.tipo.isEntrada()) {
            throw new ValidationException("Tipo deve ser uma entrada");
        }
        if (cmd.itens == null || cmd.itens.isEmpty()) {
            throw new ValidationException("Pelo menos um item é obrigatório");
        }
        for (var item : cmd.itens) {
            if (item.quantidadeBase == null || item.quantidadeBase.signum() <= 0) {
                throw new ValidationException("Quantidade base deve ser positiva");
            }
            if (item.valorUnitario == null || item.valorUnitario.signum() < 0) {
                throw new ValidationException("Valor unitário não pode ser negativo");
            }
            // numeroLote só é obrigatório no regime RASTREADO (controla_validade=true).
            // Quando false → roteamos para o lote AGREGADOR e ignoramos o número.
            if (item.controlaValidade && (item.numeroLote == null || item.numeroLote.isBlank())) {
                throw new ValidationException("Número do lote é obrigatório para insumo que controla validade");
            }
        }

        List<ItemMovimentacao> consolidados = new ArrayList<>();
        for (var item : cmd.itens) {
            Lote lote;
            if (item.controlaValidade) {
                lote = loteRepo.save(Lote.novoRastreado(item.insumoId, item.fornecedorId,
                        item.notaFiscalId, item.numeroLote,
                        item.dataFabricacao, item.dataValidade,
                        item.valorUnitario, clock.instant()));
            } else {
                // Lote único do insumo. NF / fornecedor ficam só no histórico
                // de movimentação. O valor unitário do agregador é atualizado
                // pelo custo médio ponderado a cada entrada (T-CMV-01) — saídas
                // (RegistrarSaidaMultiItem) leem lote.valorUnitario diretamente
                // e portanto refletem o CMV vigente no momento da venda.
                lote = agregador.executeComCusto(item.insumoId, item.quantidadeBase, item.valorUnitario);
            }

            ItemMovimentacao mi = ItemMovimentacao.novo(
                    item.insumoId, lote.id(), item.unidadeLancamentoId,
                    item.quantidadeLancada, item.quantidadeBase, item.valorUnitario);
            consolidados.add(mi);
        }

        Movimentacao mov = Movimentacao.nova(cmd.filialId, cmd.usuarioId, cmd.tipo,
                clock.instant(), cmd.documentoOrigemTipo, cmd.documentoOrigemId, cmd.observacao,
                false, consolidados, clock.instant());

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
            List<ItemEntrada> itens
    ) {}

    public record ItemEntrada(
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
            boolean controlaValidade
    ) {}
}
