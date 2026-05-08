package com.nonnas.inventory.application.ports;

import com.nonnas.inventory.domain.LoteId;
import com.nonnas.inventory.domain.SaldoLote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso ao saldo materializado por (lote, filial). O método
 * {@link #findLotesParaSaidaFefo} retorna lotes com saldo positivo
 * ordenados por data de validade — base do algoritmo FEFO.
 */
public interface SaldoLoteRepository {

    SaldoLote save(SaldoLote s);

    Optional<SaldoLote> findById(LoteId loteId, UUID filialId);

    /** Soma de saldos por insumo numa filial. Calculado por aggregate query. */
    java.math.BigDecimal somarPorInsumoEFilial(UUID insumoId, UUID filialId);

    /**
     * Retorna lotes com saldo positivo do insumo na filial, ordenados por
     * data_validade NULLS LAST, lote.id ASC. Lock pessimista para serializar
     * saídas concorrentes no mesmo lote.
     */
    List<LoteSaldoFefo> findLotesParaSaidaFefo(UUID insumoId, UUID filialId);

    /** Projeção usada exclusivamente pelo FEFO para evitar trazer agregados pesados. */
    record LoteSaldoFefo(LoteId loteId, java.math.BigDecimal saldoBase, java.time.LocalDate dataValidade) {}
}
