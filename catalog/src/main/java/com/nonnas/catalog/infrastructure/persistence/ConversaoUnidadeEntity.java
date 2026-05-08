package com.nonnas.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversoes_unidade")
public class ConversaoUnidadeEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "insumo_id")
    private UUID insumoId;

    @Column(name = "unidade_origem_id", nullable = false)
    private UUID unidadeOrigemId;

    @Column(name = "unidade_destino_id", nullable = false)
    private UUID unidadeDestinoId;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal fator;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ConversaoUnidadeEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInsumoId() { return insumoId; }
    public void setInsumoId(UUID insumoId) { this.insumoId = insumoId; }
    public UUID getUnidadeOrigemId() { return unidadeOrigemId; }
    public void setUnidadeOrigemId(UUID unidadeOrigemId) { this.unidadeOrigemId = unidadeOrigemId; }
    public UUID getUnidadeDestinoId() { return unidadeDestinoId; }
    public void setUnidadeDestinoId(UUID unidadeDestinoId) { this.unidadeDestinoId = unidadeDestinoId; }
    public BigDecimal getFator() { return fator; }
    public void setFator(BigDecimal fator) { this.fator = fator; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
