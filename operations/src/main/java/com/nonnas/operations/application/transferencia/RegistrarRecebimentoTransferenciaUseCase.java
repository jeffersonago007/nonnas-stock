package com.nonnas.operations.application.transferencia;

import com.nonnas.inventory.application.movimentacao.RegistrarEntradaMultiItemUseCase;
import com.nonnas.inventory.application.ports.LoteRepository;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.operations.application.ports.AjusteEstoqueRepository;
import com.nonnas.operations.application.ports.TransferenciaRepository;
import com.nonnas.operations.domain.AjusteEstoque;
import com.nonnas.operations.domain.ItemTransferencia;
import com.nonnas.operations.domain.Transferencia;
import com.nonnas.operations.domain.TransferenciaId;
import com.nonnas.operations.infrastructure.config.OperationsProperties;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registra recebimento: gera ENTRADA_TRANSFERENCIA na filial destino com
 * lotes novos para cada item recebido (qtd_recebida &gt; 0). Para cada
 * divergência (qtd_recebida &ne; qtd_solicitada) cria um {@link AjusteEstoque}
 * com {@code origem_transferencia_id} preenchido — apenas documenta o evento
 * para auditoria, SEM gerar movimentação adicional (a perda já está
 * implícita na diferença entre saída origem e entrada destino).
 */
@Service
public class RegistrarRecebimentoTransferenciaUseCase {

    private static final String DOC_ORIGEM_TIPO = "TRANSFERENCIA";

    private final TransferenciaRepository transferenciaRepo;
    private final AjusteEstoqueRepository ajusteRepo;
    private final RegistrarEntradaMultiItemUseCase entradaMulti;
    private final LoteRepository loteRepo;
    private final OperationsProperties props;
    private final Clock clock;

    public RegistrarRecebimentoTransferenciaUseCase(TransferenciaRepository transferenciaRepo,
                                                    AjusteEstoqueRepository ajusteRepo,
                                                    RegistrarEntradaMultiItemUseCase entradaMulti,
                                                    LoteRepository loteRepo,
                                                    OperationsProperties props,
                                                    Clock clock) {
        this.transferenciaRepo = transferenciaRepo;
        this.ajusteRepo = ajusteRepo;
        this.entradaMulti = entradaMulti;
        this.loteRepo = loteRepo;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public Transferencia execute(Comando cmd) {
        Transferencia t = transferenciaRepo.findById(TransferenciaId.of(cmd.transferenciaId))
                .orElseThrow(() -> new NotFoundException("Transferência", cmd.transferenciaId));

        // Itens recebidos com qtd > 0 viram entradas no destino
        Map<UUID, BigDecimal> qtdsRecebidas = Map.copyOf(cmd.quantidadesRecebidas);
        validarChavesItens(t, qtdsRecebidas);

        Instant agora = clock.instant();
        List<RegistrarEntradaMultiItemUseCase.ItemEntrada> entradas = new ArrayList<>();
        for (ItemTransferencia it : t.itens()) {
            BigDecimal qtdRecebida = qtdsRecebidas.getOrDefault(it.id(), BigDecimal.ZERO);
            if (qtdRecebida.signum() > 0) {
                // Decisão sobre regime sem importar catalog: se já existe
                // agregador para o insumo, regime é AGREGADOR e o saldo
                // recebido vai para esse lote. Caso contrário, mantém o
                // comportamento legado de lote rastreado TRANSF-<id>.
                boolean controlaValidade = loteRepo.findAgregadorByInsumo(it.insumoId()).isEmpty();
                entradas.add(new RegistrarEntradaMultiItemUseCase.ItemEntrada(
                        it.insumoId(), null, null,
                        "TRANSF-%s".formatted(t.id().value()),
                        null, null,
                        BigDecimal.ZERO,  // valor unitário do recebimento; valor real fica no lote origem
                        it.unidadeId(), qtdRecebida, qtdRecebida,
                        controlaValidade));
            }
        }

        UUID movEntradaId;
        if (entradas.isEmpty()) {
            throw new ValidationException("Recebimento não pode ser feito sem nenhum item recebido");
        }
        Movimentacao mov = entradaMulti.execute(new RegistrarEntradaMultiItemUseCase.Comando(
                t.filialDestinoId(), cmd.recebidoPor, TipoMovimentacao.ENTRADA_TRANSFERENCIA,
                DOC_ORIGEM_TIPO, t.id().value(), t.observacaoOpt().orElse(null), entradas));
        movEntradaId = mov.id().value();

        t.registrarRecebimento(cmd.recebidoPor, qtdsRecebidas, movEntradaId, agora);

        // Documenta divergências como AjusteEstoque APROVADO sem movimentação
        for (ItemTransferencia it : t.itensComDivergencia()) {
            BigDecimal diff = it.divergencia();  // qtdRecebida - qtdSolicitada (negativo se faltou)
            AjusteEstoque ajuste = AjusteEstoque.novo(
                    t.filialDestinoId(), it.insumoId(), it.unidadeId(),
                    diff, "Divergência transferência %s".formatted(t.id().value()),
                    cmd.recebidoPor, props.ajuste().thresholdAprovacao(),
                    t.id().value(), agora);
            ajusteRepo.save(ajuste);
        }

        return transferenciaRepo.save(t);
    }

    private void validarChavesItens(Transferencia t, Map<UUID, BigDecimal> qtds) {
        var itemIds = t.itens().stream().map(ItemTransferencia::id).toList();
        for (UUID k : qtds.keySet()) {
            if (!itemIds.contains(k)) {
                throw new ValidationException("Item informado no recebimento não pertence à transferência: " + k);
            }
        }
    }

    public record Comando(
            UUID transferenciaId,
            UUID recebidoPor,
            Map<UUID, BigDecimal> quantidadesRecebidas
    ) {}
}
