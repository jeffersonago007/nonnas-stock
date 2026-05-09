package com.nonnas.recipes.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataProdutoVendavelRepository extends JpaRepository<ProdutoVendavelEntity, UUID> {
    Optional<ProdutoVendavelEntity> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
