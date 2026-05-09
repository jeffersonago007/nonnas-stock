package com.nonnas.inventory.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataSaldoLoteRepository extends JpaRepository<SaldoLoteEntity, SaldoLoteEntity.SaldoLoteId> {

    @Query("""
        SELECT COALESCE(SUM(s.quantidadeBase), 0) FROM SaldoLoteEntity s
        JOIN LoteEntity l ON l.id = s.loteId
        WHERE l.insumoId = :insumoId AND s.filialId = :filialId
        """)
    BigDecimal somarPorInsumoEFilial(@Param("insumoId") UUID insumoId, @Param("filialId") UUID filialId);

    /**
     * FEFO: lotes com saldo positivo do insumo na filial, ordenados por
     * data de validade (NULLS LAST) e id. Lock pessimista serializa saídas
     * concorrentes no mesmo lote.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT new com.nonnas.inventory.infrastructure.persistence.SpringDataSaldoLoteRepository$LoteFefoRow(
            s.loteId, s.quantidadeBase, l.dataValidade)
        FROM SaldoLoteEntity s
        JOIN LoteEntity l ON l.id = s.loteId
        WHERE l.insumoId = :insumoId
          AND s.filialId = :filialId
          AND s.quantidadeBase > 0
        ORDER BY (CASE WHEN l.dataValidade IS NULL THEN 1 ELSE 0 END), l.dataValidade ASC, l.id ASC
        """)
    List<LoteFefoRow> findLotesParaSaidaFefo(@Param("insumoId") UUID insumoId, @Param("filialId") UUID filialId);

    Optional<SaldoLoteEntity> findByLoteIdAndFilialId(UUID loteId, UUID filialId);

    @Query("""
        SELECT new com.nonnas.inventory.infrastructure.persistence.SpringDataSaldoLoteRepository$LoteVencendoRow(
            s.loteId, l.insumoId, s.filialId, s.quantidadeBase, l.dataValidade)
        FROM SaldoLoteEntity s
        JOIN LoteEntity l ON l.id = s.loteId
        WHERE l.dataValidade IS NOT NULL
          AND l.dataValidade <= :ate
          AND s.quantidadeBase > 0
        """)
    List<LoteVencendoRow> findLotesVencendoComSaldoAte(@Param("ate") LocalDate ate);

    record LoteFefoRow(UUID loteId, BigDecimal saldoBase, LocalDate dataValidade) {}

    record LoteVencendoRow(UUID loteId, UUID insumoId, UUID filialId, BigDecimal saldoBase, LocalDate dataValidade) {}
}
