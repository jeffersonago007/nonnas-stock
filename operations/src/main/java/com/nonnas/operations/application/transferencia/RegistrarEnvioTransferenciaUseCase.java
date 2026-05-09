package com.nonnas.operations.application.transferencia;

import com.nonnas.inventory.application.movimentacao.RegistrarSaidaMultiItemUseCase;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.operations.application.ports.TransferenciaRepository;
import com.nonnas.operations.domain.Transferencia;
import com.nonnas.operations.domain.TransferenciaId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Registra envio: gera SAIDA_TRANSFERENCIA na filial origem com todos os
 * itens via FEFO ({@link RegistrarSaidaMultiItemUseCase}). Tudo na mesma
 * transação — se algum insumo não tem lote, falha e estado da transferência
 * volta para APROVADA (rollback).
 */
@Service
public class RegistrarEnvioTransferenciaUseCase {

    private static final String DOC_ORIGEM_TIPO = "TRANSFERENCIA";

    private final TransferenciaRepository repo;
    private final RegistrarSaidaMultiItemUseCase saidaMulti;
    private final Clock clock;

    public RegistrarEnvioTransferenciaUseCase(TransferenciaRepository repo,
                                              RegistrarSaidaMultiItemUseCase saidaMulti,
                                              Clock clock) {
        this.repo = repo;
        this.saidaMulti = saidaMulti;
        this.clock = clock;
    }

    @Transactional
    public Transferencia execute(UUID transferenciaId, UUID enviadoPor) {
        Transferencia t = repo.findById(TransferenciaId.of(transferenciaId))
                .orElseThrow(() -> new NotFoundException("Transferência", transferenciaId));

        var itens = t.itens().stream()
                .map(i -> new RegistrarSaidaMultiItemUseCase.ItemSaida(
                        i.insumoId(), i.unidadeId(), i.quantidadeSolicitada()))
                .toList();
        var cmd = new RegistrarSaidaMultiItemUseCase.Comando(
                t.filialOrigemId(), enviadoPor, TipoMovimentacao.SAIDA_TRANSFERENCIA,
                DOC_ORIGEM_TIPO, t.id().value(), t.observacaoOpt().orElse(null), itens);
        Movimentacao mov = saidaMulti.execute(cmd);

        t.registrarEnvio(enviadoPor, mov.id().value(), clock.instant());
        return repo.save(t);
    }
}
