package com.nonnas.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataInsumoRepository extends JpaRepository<InsumoEntity, UUID> {
    Optional<InsumoEntity> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
