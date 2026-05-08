package com.nonnas.inventory.infrastructure.persistence;

import com.nonnas.inventory.application.ports.LoteRepository;
import com.nonnas.inventory.domain.Lote;
import com.nonnas.inventory.domain.LoteId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class LoteRepositoryImpl implements LoteRepository {

    private final SpringDataLoteRepository jpa;

    LoteRepositoryImpl(SpringDataLoteRepository jpa) { this.jpa = jpa; }

    @Override public Lote save(Lote l) {
        return InventoryMappers.toDomain(jpa.save(InventoryMappers.toEntity(l)));
    }
    @Override public Optional<Lote> findById(LoteId id) {
        return jpa.findById(id.value()).map(InventoryMappers::toDomain);
    }
    @Override public List<Lote> findByInsumo(UUID insumoId, int page, int size) {
        return jpa.findByInsumoId(insumoId, PageRequest.of(page, size))
                .map(InventoryMappers::toDomain).getContent();
    }
}
