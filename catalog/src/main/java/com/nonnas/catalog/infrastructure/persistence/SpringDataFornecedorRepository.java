package com.nonnas.catalog.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataFornecedorRepository extends JpaRepository<FornecedorEntity, UUID> {
    Optional<FornecedorEntity> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);

    @Query("""
            SELECT f FROM FornecedorEntity f
            WHERE (:ativo IS NULL OR f.ativo = :ativo)
              AND (:q IS NULL OR LOWER(f.razaoSocial) LIKE :q
                              OR f.cnpj LIKE :q)
            ORDER BY f.razaoSocial ASC
            """)
    Page<FornecedorEntity> findFiltered(@Param("ativo") Boolean ativo,
                                        @Param("q") String q,
                                        Pageable pageable);
}
