package com.nonnas.operations.application.transferencia;

import com.nonnas.operations.application.ports.TransferenciaRepository;
import com.nonnas.operations.domain.ItemTransferencia;
import com.nonnas.operations.domain.Transferencia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class SolicitarTransferenciaUseCase {

    private final TransferenciaRepository repo;
    private final Clock clock;

    public SolicitarTransferenciaUseCase(TransferenciaRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional
    public Transferencia execute(Comando cmd) {
        List<ItemTransferencia> itens = cmd.itens.stream()
                .map(i -> ItemTransferencia.novo(i.insumoId, i.unidadeId, i.quantidade))
                .toList();
        Transferencia t = Transferencia.solicitar(
                cmd.filialOrigemId, cmd.filialDestinoId, cmd.solicitadoPor,
                itens, cmd.observacao, clock.instant());
        return repo.save(t);
    }

    public record Comando(
            UUID filialOrigemId,
            UUID filialDestinoId,
            UUID solicitadoPor,
            String observacao,
            List<ItemEntrada> itens
    ) {}

    public record ItemEntrada(UUID insumoId, UUID unidadeId, BigDecimal quantidade) {}
}
