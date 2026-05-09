package com.nonnas.recipes.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "items_ficha_tecnica")
public class ItemFichaTecnicaEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "insumo_id", nullable = false, updatable = false) private UUID insumoId;
    @Column(name = "unidade_id", nullable = false, updatable = false) private UUID unidadeId;
    @Column(nullable = false, precision = 20, scale = 4, updatable = false) private BigDecimal quantidade;

    public ItemFichaTecnicaEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getInsumoId() { return insumoId; } public void setInsumoId(UUID v) { this.insumoId = v; }
    public UUID getUnidadeId() { return unidadeId; } public void setUnidadeId(UUID v) { this.unidadeId = v; }
    public BigDecimal getQuantidade() { return quantidade; } public void setQuantidade(BigDecimal v) { this.quantidade = v; }
}
