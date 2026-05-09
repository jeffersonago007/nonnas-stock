package com.nonnas.recipes.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "produtos_vendaveis")
public class ProdutoVendavelEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(nullable = false, length = 50) private String codigo;
    @Column(nullable = false, length = 150) private String nome;
    @Column(nullable = false, length = 50) private String categoria;
    @Column(nullable = false) private boolean ativo;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public ProdutoVendavelEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public String getCodigo() { return codigo; } public void setCodigo(String v) { this.codigo = v; }
    public String getNome() { return nome; } public void setNome(String v) { this.nome = v; }
    public String getCategoria() { return categoria; } public void setCategoria(String v) { this.categoria = v; }
    public boolean isAtivo() { return ativo; } public void setAtivo(boolean v) { this.ativo = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
