package com.nonnas.operations.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "notas_fiscais")
public class NotaFiscalEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "fornecedor_id", nullable = false, updatable = false) private UUID fornecedorId;
    @Column(name = "filial_id", nullable = false, updatable = false) private UUID filialId;
    @Column(nullable = false, length = 20, updatable = false) private String numero;
    @Column(nullable = false, length = 10, updatable = false) private String serie;
    @Column(name = "chave_nfe", length = 44, updatable = false) private String chaveNfe;
    @Column(name = "data_emissao", nullable = false, updatable = false) private Instant dataEmissao;
    @Column(name = "data_lancamento", nullable = false, updatable = false) private Instant dataLancamento;
    @Column(name = "valor_total", nullable = false, precision = 20, scale = 2, updatable = false) private BigDecimal valorTotal;
    @Column private String observacao;
    @Column(name = "created_by_usuario_id", nullable = false, updatable = false) private UUID createdByUsuarioId;
    @Column(name = "movimentacao_entrada_id", nullable = false, updatable = false) private UUID movimentacaoEntradaId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "nota_fiscal_id", nullable = false, updatable = false)
    private List<ItemNotaFiscalEntity> itens = new ArrayList<>();

    public NotaFiscalEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getFornecedorId() { return fornecedorId; } public void setFornecedorId(UUID v) { this.fornecedorId = v; }
    public UUID getFilialId() { return filialId; } public void setFilialId(UUID v) { this.filialId = v; }
    public String getNumero() { return numero; } public void setNumero(String v) { this.numero = v; }
    public String getSerie() { return serie; } public void setSerie(String v) { this.serie = v; }
    public String getChaveNfe() { return chaveNfe; } public void setChaveNfe(String v) { this.chaveNfe = v; }
    public Instant getDataEmissao() { return dataEmissao; } public void setDataEmissao(Instant v) { this.dataEmissao = v; }
    public Instant getDataLancamento() { return dataLancamento; } public void setDataLancamento(Instant v) { this.dataLancamento = v; }
    public BigDecimal getValorTotal() { return valorTotal; } public void setValorTotal(BigDecimal v) { this.valorTotal = v; }
    public String getObservacao() { return observacao; } public void setObservacao(String v) { this.observacao = v; }
    public UUID getCreatedByUsuarioId() { return createdByUsuarioId; } public void setCreatedByUsuarioId(UUID v) { this.createdByUsuarioId = v; }
    public UUID getMovimentacaoEntradaId() { return movimentacaoEntradaId; } public void setMovimentacaoEntradaId(UUID v) { this.movimentacaoEntradaId = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }
    public List<ItemNotaFiscalEntity> getItens() { return itens; } public void setItens(List<ItemNotaFiscalEntity> v) { this.itens = v; }
}
