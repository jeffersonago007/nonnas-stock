package com.nonnas.operations.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "notas_fiscais_itens")
public class ItemNotaFiscalEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "insumo_id", nullable = false, updatable = false) private UUID insumoId;
    @Column(name = "codigo_fornecedor", length = 60, updatable = false) private String codigoFornecedor;
    @Column(name = "descricao_origem", nullable = false, length = 500, updatable = false) private String descricaoOrigem;
    @Column(nullable = false, precision = 20, scale = 4, updatable = false) private BigDecimal quantidade;
    @Column(name = "unidade_medida_id", nullable = false, updatable = false) private UUID unidadeMedidaId;
    @Column(name = "valor_unitario", nullable = false, precision = 20, scale = 4, updatable = false) private BigDecimal valorUnitario;
    @Column(name = "valor_total", nullable = false, precision = 20, scale = 2, updatable = false) private BigDecimal valorTotal;
    @Column(length = 60, updatable = false) private String lote;
    @Column(name = "data_validade", updatable = false) private LocalDate dataValidade;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public ItemNotaFiscalEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getInsumoId() { return insumoId; } public void setInsumoId(UUID v) { this.insumoId = v; }
    public String getCodigoFornecedor() { return codigoFornecedor; } public void setCodigoFornecedor(String v) { this.codigoFornecedor = v; }
    public String getDescricaoOrigem() { return descricaoOrigem; } public void setDescricaoOrigem(String v) { this.descricaoOrigem = v; }
    public BigDecimal getQuantidade() { return quantidade; } public void setQuantidade(BigDecimal v) { this.quantidade = v; }
    public UUID getUnidadeMedidaId() { return unidadeMedidaId; } public void setUnidadeMedidaId(UUID v) { this.unidadeMedidaId = v; }
    public BigDecimal getValorUnitario() { return valorUnitario; } public void setValorUnitario(BigDecimal v) { this.valorUnitario = v; }
    public BigDecimal getValorTotal() { return valorTotal; } public void setValorTotal(BigDecimal v) { this.valorTotal = v; }
    public String getLote() { return lote; } public void setLote(String v) { this.lote = v; }
    public LocalDate getDataValidade() { return dataValidade; } public void setDataValidade(LocalDate v) { this.dataValidade = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
}
