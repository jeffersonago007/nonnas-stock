package com.nonnas.inventory.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "items_movimentacao")
public class ItemMovimentacaoEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "movimentacao_id", nullable = false, updatable = false) private UUID movimentacaoId;
    @Column(name = "insumo_id", nullable = false, updatable = false) private UUID insumoId;
    @Column(name = "lote_id", nullable = false, updatable = false) private UUID loteId;
    @Column(name = "unidade_lancamento_id", nullable = false, updatable = false) private UUID unidadeLancamentoId;
    @Column(name = "quantidade_lancada", nullable = false, precision = 20, scale = 4, updatable = false) private BigDecimal quantidadeLancada;
    @Column(name = "quantidade_base", nullable = false, precision = 20, scale = 4, updatable = false) private BigDecimal quantidadeBase;
    @Column(name = "valor_unitario", nullable = false, precision = 20, scale = 4, updatable = false) private BigDecimal valorUnitario;

    public ItemMovimentacaoEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getMovimentacaoId() { return movimentacaoId; } public void setMovimentacaoId(UUID v) { this.movimentacaoId = v; }
    public UUID getInsumoId() { return insumoId; } public void setInsumoId(UUID v) { this.insumoId = v; }
    public UUID getLoteId() { return loteId; } public void setLoteId(UUID v) { this.loteId = v; }
    public UUID getUnidadeLancamentoId() { return unidadeLancamentoId; } public void setUnidadeLancamentoId(UUID v) { this.unidadeLancamentoId = v; }
    public BigDecimal getQuantidadeLancada() { return quantidadeLancada; } public void setQuantidadeLancada(BigDecimal v) { this.quantidadeLancada = v; }
    public BigDecimal getQuantidadeBase() { return quantidadeBase; } public void setQuantidadeBase(BigDecimal v) { this.quantidadeBase = v; }
    public BigDecimal getValorUnitario() { return valorUnitario; } public void setValorUnitario(BigDecimal v) { this.valorUnitario = v; }
}
