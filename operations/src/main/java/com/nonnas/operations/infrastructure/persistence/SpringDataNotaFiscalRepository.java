package com.nonnas.operations.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataNotaFiscalRepository extends JpaRepository<NotaFiscalEntity, UUID> {

    Optional<NotaFiscalEntity> findByChaveNfe(String chaveNfe);

    boolean existsByChaveNfe(String chaveNfe);

    // findFiltered foi movido para NotaFiscalRepositoryImpl com Criteria
    // dinâmico (Postgres rejeita ":param IS NULL" sem cast de tipo).
}
