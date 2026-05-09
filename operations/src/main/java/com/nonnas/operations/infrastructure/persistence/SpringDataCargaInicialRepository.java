package com.nonnas.operations.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataCargaInicialRepository extends JpaRepository<CargaInicialEntity, UUID> {
    Optional<CargaInicialEntity> findByHashPlanilha(String hash);
}
