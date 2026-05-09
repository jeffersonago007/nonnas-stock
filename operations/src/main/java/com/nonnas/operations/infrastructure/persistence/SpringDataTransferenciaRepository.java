package com.nonnas.operations.infrastructure.persistence;

import com.nonnas.operations.domain.StatusTransferencia;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

interface SpringDataTransferenciaRepository extends JpaRepository<TransferenciaEntity, UUID> {

    List<TransferenciaEntity> findByFilialOrigemIdOrderByDataSolicitacaoDesc(UUID filialOrigemId, Pageable pageable);

    List<TransferenciaEntity> findByStatusOrderByDataSolicitacaoDesc(StatusTransferencia status, Pageable pageable);

    @Query("""
            SELECT new com.nonnas.operations.infrastructure.persistence.SpringDataTransferenciaRepository$EmTransitoRow(
                i.insumoId, SUM(i.quantidadeSolicitada))
            FROM TransferenciaEntity t JOIN t.itens i
            WHERE t.status = 'EM_TRANSITO'
              AND (:filialDestino IS NULL OR t.filialDestinoId = :filialDestino)
            GROUP BY i.insumoId
            """)
    List<EmTransitoRow> agregadoEmTransito(@Param("filialDestino") UUID filialDestino);

    record EmTransitoRow(UUID insumoId, BigDecimal quantidadeEmTransito) {}
}
