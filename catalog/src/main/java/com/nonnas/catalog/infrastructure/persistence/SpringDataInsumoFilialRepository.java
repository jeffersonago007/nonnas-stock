package com.nonnas.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataInsumoFilialRepository extends JpaRepository<InsumoFilialEntity, UUID> {
    boolean existsByInsumoIdAndFilialId(UUID insumoId, UUID filialId);
    java.util.Optional<InsumoFilialEntity> findByInsumoIdAndFilialId(UUID insumoId, UUID filialId);
}
