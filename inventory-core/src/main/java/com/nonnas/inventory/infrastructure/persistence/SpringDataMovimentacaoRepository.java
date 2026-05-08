package com.nonnas.inventory.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataMovimentacaoRepository extends JpaRepository<MovimentacaoEntity, UUID> {
    Page<MovimentacaoEntity> findByFilialIdOrderByDataMovimentacaoDesc(UUID filialId, Pageable pageable);
}
