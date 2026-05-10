package com.nonnas.inventory.application.ports;

import com.nonnas.inventory.domain.Lote;
import com.nonnas.inventory.domain.LoteId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoteRepository {
    Lote save(Lote l);
    Optional<Lote> findById(LoteId id);
    List<Lote> findByInsumo(UUID insumoId, int page, int size);

    /**
     * Retorna o lote AGREGADOR único do insumo, se já existe. Schema garante
     * unicidade via index parcial (V020 inventory-core). Lookup é o caminho
     * quente do {@code BuscarOuCriarLoteAgregadorUseCase}.
     */
    Optional<Lote> findAgregadorByInsumo(UUID insumoId);
}
