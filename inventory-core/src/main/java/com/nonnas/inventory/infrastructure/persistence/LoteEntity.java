package com.nonnas.inventory.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "lotes")
public class LoteEntity {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "insumo_id", nullable = false) private UUID insumoId;
    @Column(name = "tipo", nullable = false, length = 20) private String tipo;
    @Column(name = "fornecedor_id") private UUID fornecedorId;
    @Column(name = "nota_fiscal_id") private UUID notaFiscalId;
    @Column(name = "numero_lote") private String numeroLote;
    @Column(name = "data_fabricacao") private LocalDate dataFabricacao;
    @Column(name = "data_validade") private LocalDate dataValidade;
    @Column(name = "valor_unitario", nullable = false, precision = 20, scale = 4) private BigDecimal valorUnitario;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public LoteEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getInsumoId() { return insumoId; } public void setInsumoId(UUID v) { this.insumoId = v; }
    public String getTipo() { return tipo; } public void setTipo(String v) { this.tipo = v; }
    public UUID getFornecedorId() { return fornecedorId; } public void setFornecedorId(UUID v) { this.fornecedorId = v; }
    public UUID getNotaFiscalId() { return notaFiscalId; } public void setNotaFiscalId(UUID v) { this.notaFiscalId = v; }
    public String getNumeroLote() { return numeroLote; } public void setNumeroLote(String v) { this.numeroLote = v; }
    public LocalDate getDataFabricacao() { return dataFabricacao; } public void setDataFabricacao(LocalDate v) { this.dataFabricacao = v; }
    public LocalDate getDataValidade() { return dataValidade; } public void setDataValidade(LocalDate v) { this.dataValidade = v; }
    public BigDecimal getValorUnitario() { return valorUnitario; } public void setValorUnitario(BigDecimal v) { this.valorUnitario = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
}
