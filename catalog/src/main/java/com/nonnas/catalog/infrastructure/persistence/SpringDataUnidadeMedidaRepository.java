package com.nonnas.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataUnidadeMedidaRepository extends JpaRepository<UnidadeMedidaEntity, UUID> {
    Optional<UnidadeMedidaEntity> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
