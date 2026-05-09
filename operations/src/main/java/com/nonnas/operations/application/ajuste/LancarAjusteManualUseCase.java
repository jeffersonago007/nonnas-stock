package com.nonnas.operations.application.ajuste;

import com.nonnas.inventory.application.movimentacao.RegistrarEntradaManualUseCase;
import com.nonnas.inventory.application.movimentacao.RegistrarSaidaManualUseCase;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.operations.application.ports.AjusteEstoqueRepository;
import com.nonnas.operations.domain.AjusteEstoque;
import com.nonnas.operations.infrastructure.config.OperationsProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

/**
 * Lança ajuste manual. Se {@code |quantidade_diff| > threshold}, cria
 * {@link AjusteEstoque} {@code PENDENTE_APROVACAO} e não gera movimentação
 * — fluxo de aprovação fica para {@code AprovarAjusteEstoqueUseCase}.
 *
 * <p>Caso contrário, cria já {@code APROVADO} e dispara movimentação
 * imediatamente: {@code ENTRADA_AJUSTE} (diff positivo) ou
 * {@code SAIDA_AJUSTE} (diff negativo, baixa via FEFO).
 *
 * <p>Para ajuste positivo, cria-se um lote sintético {@code AJUSTE-<id>}
 * com valor unitário zero — o que entrou não tem nota fiscal nem fornecedor
 * de origem (ajuste manual é correção, não entrada comercial).
 */
@Service
public class LancarAjusteManualUseCase {

    private static final String DOC_ORIGEM_TIPO = "AJUSTE_ESTOQUE";

    private final AjusteEstoqueRepository ajusteRepo;
    private final RegistrarEntradaManualUseCase entradaManual;
    private final RegistrarSaidaManualUseCase saidaManual;
    private final OperationsProperties props;
    private final Clock clock;

    public LancarAjusteManualUseCase(AjusteEstoqueRepository ajusteRepo,
                                     RegistrarEntradaManualUseCase entradaManual,
                                     RegistrarSaidaManualUseCase saidaManual,
                                     OperationsProperties props,
                                     Clock clock) {
        this.ajusteRepo = ajusteRepo;
        this.entradaManual = entradaManual;
        this.saidaManual = saidaManual;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public AjusteEstoque execute(Comando cmd) {
        AjusteEstoque ajuste = AjusteEstoque.novo(
                cmd.filialId, cmd.insumoId, cmd.unidadeId,
                cmd.quantidadeDiff, cmd.motivo, cmd.solicitadoPor,
                props.ajuste().thresholdAprovacao(), null, clock.instant());

        if (ajuste.requerAprovacao()) {
            return ajusteRepo.save(ajuste);
        }

        UUID movId = gerarMovimentacao(ajuste);
        ajuste.anexarMovimentacao(movId, clock.instant());
        return ajusteRepo.save(ajuste);
    }

    private UUID gerarMovimentacao(AjusteEstoque ajuste) {
        BigDecimal qtdAbs = ajuste.quantidadeDiff().abs();
        if (ajuste.quantidadeDiff().signum() > 0) {
            // Entrada — cria lote sintético
            var entradaCmd = new RegistrarEntradaManualUseCase.Comando(
                    ajuste.filialId(), ajuste.solicitadoPor(),
                    ajuste.insumoId(), null, null,
                    "AJUSTE-%s".formatted(ajuste.id().value()),
                    null, null, BigDecimal.ZERO,
                    ajuste.unidadeId(), qtdAbs, qtdAbs,
                    TipoMovimentacao.ENTRADA_AJUSTE,
                    DOC_ORIGEM_TIPO, ajuste.id().value(), ajuste.motivo());
            Movimentacao mov = entradaManual.execute(entradaCmd);
            return mov.id().value();
        } else {
            // Saída via FEFO
            var saidaCmd = new RegistrarSaidaManualUseCase.Comando(
                    ajuste.filialId(), ajuste.solicitadoPor(),
                    ajuste.insumoId(), ajuste.unidadeId(), qtdAbs,
                    TipoMovimentacao.SAIDA_AJUSTE,
                    DOC_ORIGEM_TIPO, ajuste.id().value(), ajuste.motivo());
            Movimentacao mov = saidaManual.execute(saidaCmd);
            return mov.id().value();
        }
    }

    public record Comando(
            UUID filialId,
            UUID insumoId,
            UUID unidadeId,
            BigDecimal quantidadeDiff,
            String motivo,
            UUID solicitadoPor
    ) {}
}
