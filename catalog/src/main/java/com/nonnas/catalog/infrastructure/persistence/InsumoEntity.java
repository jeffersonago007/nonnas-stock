package com.nonnas.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "insumos")
public class InsumoEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    @Column(name = "categoria_id", nullable = false)
    private UUID categoriaId;

    @Column(name = "unidade_base_id", nullable = false)
    private UUID unidadeBaseId;

    @Column(name = "controla_lote", nullable = false)
    private boolean controlaLote;

    @Column(name = "controla_validade", nullable = false)
    private boolean controlaValidade;

    @Column(name = "dias_alerta_vencimento")
    private Integer diasAlertaVencimento;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InsumoEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public UUID getCategoriaId() { return categoriaId; }
    public void setCategoriaId(UUID categoriaId) { this.categoriaId = categoriaId; }
    public UUID getUnidadeBaseId() { return unidadeBaseId; }
    public void setUnidadeBaseId(UUID unidadeBaseId) { this.unidadeBaseId = unidadeBaseId; }
    public boolean isControlaLote() { return controlaLote; }
    public void setControlaLote(boolean controlaLote) { this.controlaLote = controlaLote; }
    public boolean isControlaValidade() { return controlaValidade; }
    public void setControlaValidade(boolean controlaValidade) { this.controlaValidade = controlaValidade; }
    public Integer getDiasAlertaVencimento() { return diasAlertaVencimento; }
    public void setDiasAlertaVencimento(Integer diasAlertaVencimento) { this.diasAlertaVencimento = diasAlertaVencimento; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
