package com.nonnas.inventory.infrastructure.persistence;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "saldos_lotes")
@IdClass(SaldoLoteEntity.SaldoLoteId.class)
public class SaldoLoteEntity {

    @Id @Column(name = "lote_id", nullable = false) private UUID loteId;
    @Id @Column(name = "filial_id", nullable = false) private UUID filialId;
    @Column(name = "quantidade_base", nullable = false, precision = 20, scale = 4) private BigDecimal quantidadeBase;
    @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm;

    public SaldoLoteEntity() {}

    public UUID getLoteId() { return loteId; } public void setLoteId(UUID v) { this.loteId = v; }
    public UUID getFilialId() { return filialId; } public void setFilialId(UUID v) { this.filialId = v; }
    public BigDecimal getQuantidadeBase() { return quantidadeBase; } public void setQuantidadeBase(BigDecimal v) { this.quantidadeBase = v; }
    public Instant getAtualizadoEm() { return atualizadoEm; } public void setAtualizadoEm(Instant v) { this.atualizadoEm = v; }

    public static class SaldoLoteId implements Serializable {
        private UUID loteId;
        private UUID filialId;
        public SaldoLoteId() {}
        public SaldoLoteId(UUID loteId, UUID filialId) { this.loteId = loteId; this.filialId = filialId; }
        public UUID getLoteId() { return loteId; } public void setLoteId(UUID v) { this.loteId = v; }
        public UUID getFilialId() { return filialId; } public void setFilialId(UUID v) { this.filialId = v; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof SaldoLoteId other)) return false;
            return Objects.equals(loteId, other.loteId) && Objects.equals(filialId, other.filialId);
        }
        @Override public int hashCode() { return Objects.hash(loteId, filialId); }
    }
}
