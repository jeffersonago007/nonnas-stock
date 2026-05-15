package com.nonnas.saleschannels.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "itens_pedido_canal")
public class ItemPedidoCanalEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(nullable = false) private int sequencia;
    @Column(name = "external_code", length = 120) private String externalCode;
    @Column(nullable = false, length = 300) private String nome;
    @Column(nullable = false, precision = 20, scale = 4) private BigDecimal quantidade;
    @Column(nullable = false, length = 20) private String unidade;
    @Column(name = "preco_unitario", nullable = false, precision = 20, scale = 4) private BigDecimal precoUnitario;
    @Column(name = "preco_total", nullable = false, precision = 20, scale = 4) private BigDecimal precoTotal;
    @Column(columnDefinition = "text") private String observacao;
    @Column(name = "produto_vendavel_id") private UUID produtoVendavelId;

    public ItemPedidoCanalEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public int getSequencia() { return sequencia; } public void setSequencia(int v) { this.sequencia = v; }
    public String getExternalCode() { return externalCode; } public void setExternalCode(String v) { this.externalCode = v; }
    public String getNome() { return nome; } public void setNome(String v) { this.nome = v; }
    public BigDecimal getQuantidade() { return quantidade; } public void setQuantidade(BigDecimal v) { this.quantidade = v; }
    public String getUnidade() { return unidade; } public void setUnidade(String v) { this.unidade = v; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; } public void setPrecoUnitario(BigDecimal v) { this.precoUnitario = v; }
    public BigDecimal getPrecoTotal() { return precoTotal; } public void setPrecoTotal(BigDecimal v) { this.precoTotal = v; }
    public String getObservacao() { return observacao; } public void setObservacao(String v) { this.observacao = v; }
    public UUID getProdutoVendavelId() { return produtoVendavelId; } public void setProdutoVendavelId(UUID v) { this.produtoVendavelId = v; }
}
