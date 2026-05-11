package com.nonnas.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataCategoriaInsumoRepository extends JpaRepository<CategoriaInsumoEntity, UUID> {
}
