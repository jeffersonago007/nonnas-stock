package com.nonnas.catalog.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataInsumoRepository extends JpaRepository<InsumoEntity, UUID> {
    Optional<InsumoEntity> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);

    @Query("""
            SELECT i FROM InsumoEntity i
            WHERE (:categoriaId IS NULL OR i.categoriaId = :categoriaId)
              AND (:ativo IS NULL OR i.ativo = :ativo)
              AND (:q IS NULL OR LOWER(i.nome) LIKE :q
                              OR LOWER(i.codigo) LIKE :q)
            ORDER BY i.nome ASC
            """)
    Page<InsumoEntity> findFiltered(@Param("categoriaId") UUID categoriaId,
                                    @Param("ativo") Boolean ativo,
                                    @Param("q") String q,
                                    Pageable pageable);
}
