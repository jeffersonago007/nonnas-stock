package com.nonnas.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataItemMovimentacaoRepository extends JpaRepository<ItemMovimentacaoEntity, UUID> {
    List<ItemMovimentacaoEntity> findByMovimentacaoId(UUID movimentacaoId);
}
