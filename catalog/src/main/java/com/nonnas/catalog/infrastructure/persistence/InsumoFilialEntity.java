package com.nonnas.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "insumos_filiais")
public class InsumoFilialEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "insumo_id", nullable = false)
    private UUID insumoId;

    @Column(name = "filial_id", nullable = false)
    private UUID filialId;

    @Column(name = "estoque_minimo", nullable = false, precision = 20, scale = 4)
    private BigDecimal estoqueMinimo;

    @Column(name = "estoque_maximo", precision = 20, scale = 4)
    private BigDecimal estoqueMaximo;

    @Column(name = "ponto_pedido", precision = 20, scale = 4)
    private BigDecimal pontoPedido;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InsumoFilialEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInsumoId() { return insumoId; }
    public void setInsumoId(UUID insumoId) { this.insumoId = insumoId; }
    public UUID getFilialId() { return filialId; }
    public void setFilialId(UUID filialId) { this.filialId = filialId; }
    public BigDecimal getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(BigDecimal estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }
    public BigDecimal getEstoqueMaximo() { return estoqueMaximo; }
    public void setEstoqueMaximo(BigDecimal estoqueMaximo) { this.estoqueMaximo = estoqueMaximo; }
    public BigDecimal getPontoPedido() { return pontoPedido; }
    public void setPontoPedido(BigDecimal pontoPedido) { this.pontoPedido = pontoPedido; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
