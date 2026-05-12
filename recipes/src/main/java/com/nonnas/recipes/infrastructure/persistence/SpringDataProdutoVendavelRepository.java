package com.nonnas.recipes.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataProdutoVendavelRepository extends JpaRepository<ProdutoVendavelEntity, UUID> {
    Optional<ProdutoVendavelEntity> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);

    @Query("""
            SELECT p FROM ProdutoVendavelEntity p
            WHERE (:categoria IS NULL OR p.categoria = :categoria)
              AND (:ativo IS NULL OR p.ativo = :ativo)
              AND (:tipo IS NULL OR p.tipo = :tipo)
              AND (:q IS NULL OR LOWER(p.nome) LIKE :q
                              OR LOWER(p.codigo) LIKE :q)
            ORDER BY p.nome ASC
            """)
    Page<ProdutoVendavelEntity> findFiltered(@Param("categoria") String categoria,
                                             @Param("ativo") Boolean ativo,
                                             @Param("tipo") String tipo,
                                             @Param("q") String q,
                                             Pageable pageable);

    @Query("SELECT DISTINCT p.categoria FROM ProdutoVendavelEntity p ORDER BY p.categoria ASC")
    java.util.List<String> findDistinctCategorias();

    @Query("SELECT p.insumoRevendaId FROM ProdutoVendavelEntity p WHERE p.insumoRevendaId IS NOT NULL AND p.ativo = TRUE")
    java.util.List<UUID> findAllInsumoRevendaIds();

    @Query("""
            SELECT p FROM ProdutoVendavelEntity p
            WHERE p.insumoRevendaId = :insumoId
            ORDER BY p.ativo DESC, p.updatedAt DESC
            """)
    java.util.List<ProdutoVendavelEntity> findRevendasByInsumoId(@Param("insumoId") UUID insumoId);
}
