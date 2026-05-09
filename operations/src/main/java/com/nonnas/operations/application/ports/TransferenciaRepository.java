package com.nonnas.operations.application.ports;

import com.nonnas.operations.domain.StatusTransferencia;
import com.nonnas.operations.domain.Transferencia;
import com.nonnas.operations.domain.TransferenciaId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferenciaRepository {
    Transferencia save(Transferencia t);
    Optional<Transferencia> findById(TransferenciaId id);
    List<Transferencia> findByFilialOrigem(UUID filialOrigemId, int page, int size);
    List<Transferencia> findByStatus(StatusTransferencia status, int page, int size);

    /**
     * Soma a quantidade solicitada de itens de transferências em trânsito,
     * agregado por insumo. Filtro opcional por filial destino.
     */
    List<EmTransitoPorInsumo> agregadoEmTransito(UUID filialDestinoIdOpt);

    record EmTransitoPorInsumo(UUID insumoId, BigDecimal quantidadeEmTransito) {}
}
