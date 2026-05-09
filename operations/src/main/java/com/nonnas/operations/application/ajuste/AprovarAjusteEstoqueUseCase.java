package com.nonnas.operations.application.ajuste;

import com.nonnas.inventory.application.movimentacao.RegistrarEntradaManualUseCase;
import com.nonnas.inventory.application.movimentacao.RegistrarSaidaManualUseCase;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.operations.application.ports.AjusteEstoqueRepository;
import com.nonnas.operations.domain.AjusteEstoque;
import com.nonnas.operations.domain.AjusteEstoqueId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

/**
 * Aprova um ajuste pendente: gera a movimentação correspondente
 * (ENTRADA_AJUSTE ou SAIDA_AJUSTE via FEFO) e marca o ajuste como APROVADO.
 */
@Service
public class AprovarAjusteEstoqueUseCase {

    private static final String DOC_ORIGEM_TIPO = "AJUSTE_ESTOQUE";

    private final AjusteEstoqueRepository ajusteRepo;
    private final RegistrarEntradaManualUseCase entradaManual;
    private final RegistrarSaidaManualUseCase saidaManual;
    private final Clock clock;

    public AprovarAjusteEstoqueUseCase(AjusteEstoqueRepository ajusteRepo,
                                       RegistrarEntradaManualUseCase entradaManual,
                                       RegistrarSaidaManualUseCase saidaManual,
                                       Clock clock) {
        this.ajusteRepo = ajusteRepo;
        this.entradaManual = entradaManual;
        this.saidaManual = saidaManual;
        this.clock = clock;
    }

    @Transactional
    public AjusteEstoque execute(UUID ajusteId, UUID aprovadoPor) {
        AjusteEstoque a = ajusteRepo.findById(AjusteEstoqueId.of(ajusteId))
                .orElseThrow(() -> new NotFoundException("Ajuste de estoque", ajusteId));

        UUID movId = gerarMovimentacao(a, aprovadoPor);
        a.aprovar(aprovadoPor, movId, clock.instant());
        return ajusteRepo.save(a);
    }

    private UUID gerarMovimentacao(AjusteEstoque ajuste, UUID usuarioId) {
        BigDecimal qtdAbs = ajuste.quantidadeDiff().abs();
        if (ajuste.quantidadeDiff().signum() > 0) {
            var cmd = new RegistrarEntradaManualUseCase.Comando(
                    ajuste.filialId(), usuarioId, ajuste.insumoId(), null, null,
                    "AJUSTE-%s".formatted(ajuste.id().value()),
                    null, null, BigDecimal.ZERO,
                    ajuste.unidadeId(), qtdAbs, qtdAbs,
                    TipoMovimentacao.ENTRADA_AJUSTE,
                    DOC_ORIGEM_TIPO, ajuste.id().value(), ajuste.motivo());
            Movimentacao mov = entradaManual.execute(cmd);
            return mov.id().value();
        } else {
            var cmd = new RegistrarSaidaManualUseCase.Comando(
                    ajuste.filialId(), usuarioId, ajuste.insumoId(), ajuste.unidadeId(), qtdAbs,
                    TipoMovimentacao.SAIDA_AJUSTE,
                    DOC_ORIGEM_TIPO, ajuste.id().value(), ajuste.motivo());
            Movimentacao mov = saidaManual.execute(cmd);
            return mov.id().value();
        }
    }
}
