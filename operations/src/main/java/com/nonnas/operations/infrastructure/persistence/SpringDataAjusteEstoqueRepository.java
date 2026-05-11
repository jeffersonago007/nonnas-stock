package com.nonnas.operations.infrastructure.persistence;

import com.nonnas.operations.domain.StatusAjuste;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataAjusteEstoqueRepository extends JpaRepository<AjusteEstoqueEntity, UUID> {

    List<AjusteEstoqueEntity> findByFilialIdAndStatusOrderByDataSolicitacaoDesc(
            UUID filialId, StatusAjuste status, Pageable pageable);

    List<AjusteEstoqueEntity> findByStatusOrderByDataSolicitacaoDesc(StatusAjuste status, Pageable pageable);
}
