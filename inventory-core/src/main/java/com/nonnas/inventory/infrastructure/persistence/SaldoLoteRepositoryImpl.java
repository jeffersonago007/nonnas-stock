package com.nonnas.inventory.infrastructure.persistence;

import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import com.nonnas.inventory.domain.LoteId;
import com.nonnas.inventory.domain.SaldoLote;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class SaldoLoteRepositoryImpl implements SaldoLoteRepository {

    private final SpringDataSaldoLoteRepository jpa;

    SaldoLoteRepositoryImpl(SpringDataSaldoLoteRepository jpa) { this.jpa = jpa; }

    @Override public SaldoLote save(SaldoLote s) {
        return InventoryMappers.toDomain(jpa.save(InventoryMappers.toEntity(s)));
    }

    @Override public Optional<SaldoLote> findById(LoteId loteId, UUID filialId) {
        return jpa.findByLoteIdAndFilialId(loteId.value(), filialId).map(InventoryMappers::toDomain);
    }

    @Override public BigDecimal somarPorInsumoEFilial(UUID insumoId, UUID filialId) {
        BigDecimal r = jpa.somarPorInsumoEFilial(insumoId, filialId);
        return r != null ? r : BigDecimal.ZERO;
    }

    @Override public List<LoteSaldoFefo> findLotesParaSaidaFefo(UUID insumoId, UUID filialId) {
        return jpa.findLotesParaSaidaFefo(insumoId, filialId).stream()
                .map(r -> new LoteSaldoFefo(LoteId.of(r.loteId()), r.saldoBase(), r.dataValidade()))
                .toList();
    }

    @Override public List<LoteVencendoComSaldo> findLotesVencendoComSaldoAte(java.time.LocalDate ate) {
        return jpa.findLotesVencendoComSaldoAte(ate).stream()
                .map(r -> new LoteVencendoComSaldo(
                        LoteId.of(r.loteId()), r.insumoId(), r.filialId(),
                        r.saldoBase(), r.dataValidade()))
                .toList();
    }
}
