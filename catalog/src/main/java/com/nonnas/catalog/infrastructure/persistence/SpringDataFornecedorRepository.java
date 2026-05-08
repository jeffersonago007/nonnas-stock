package com.nonnas.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataFornecedorRepository extends JpaRepository<FornecedorEntity, UUID> {
    Optional<FornecedorEntity> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);
}
