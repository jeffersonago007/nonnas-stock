package com.nonnas.operations.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fornecedor_insumo_depara")
public class FornecedorInsumoDeParaEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "fornecedor_id", nullable = false, updatable = false) private UUID fornecedorId;
    @Column(name = "codigo_fornecedor", nullable = false, length = 60, updatable = false) private String codigoFornecedor;
    @Column(name = "insumo_id", nullable = false) private UUID insumoId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "last_used_at", nullable = false) private Instant lastUsedAt;

    public FornecedorInsumoDeParaEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getFornecedorId() { return fornecedorId; } public void setFornecedorId(UUID v) { this.fornecedorId = v; }
    public String getCodigoFornecedor() { return codigoFornecedor; } public void setCodigoFornecedor(String v) { this.codigoFornecedor = v; }
    public UUID getInsumoId() { return insumoId; } public void setInsumoId(UUID v) { this.insumoId = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getLastUsedAt() { return lastUsedAt; } public void setLastUsedAt(Instant v) { this.lastUsedAt = v; }
}
