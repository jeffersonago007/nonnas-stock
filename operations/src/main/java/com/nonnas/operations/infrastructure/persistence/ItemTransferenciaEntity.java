package com.nonnas.operations.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "itens_transferencia")
public class ItemTransferenciaEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "insumo_id", nullable = false, updatable = false) private UUID insumoId;
    @Column(name = "unidade_id", nullable = false, updatable = false) private UUID unidadeId;
    @Column(name = "quantidade_solicitada", nullable = false, precision = 20, scale = 4, updatable = false) private BigDecimal quantidadeSolicitada;
    @Column(name = "quantidade_recebida", precision = 20, scale = 4) private BigDecimal quantidadeRecebida;

    public ItemTransferenciaEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getInsumoId() { return insumoId; } public void setInsumoId(UUID v) { this.insumoId = v; }
    public UUID getUnidadeId() { return unidadeId; } public void setUnidadeId(UUID v) { this.unidadeId = v; }
    public BigDecimal getQuantidadeSolicitada() { return quantidadeSolicitada; } public void setQuantidadeSolicitada(BigDecimal v) { this.quantidadeSolicitada = v; }
    public BigDecimal getQuantidadeRecebida() { return quantidadeRecebida; } public void setQuantidadeRecebida(BigDecimal v) { this.quantidadeRecebida = v; }
}
