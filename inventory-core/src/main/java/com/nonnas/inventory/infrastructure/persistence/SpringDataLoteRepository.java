package com.nonnas.inventory.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataLoteRepository extends JpaRepository<LoteEntity, UUID> {
    Page<LoteEntity> findByInsumoId(UUID insumoId, Pageable pageable);

    java.util.Optional<LoteEntity> findFirstByInsumoIdAndTipo(UUID insumoId, String tipo);
}
