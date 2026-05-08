package com.nonnas.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categorias_insumo")
public class CategoriaInsumoEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "categoria_pai_id")
    private UUID categoriaPaiId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private boolean ativa;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CategoriaInsumoEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCategoriaPaiId() { return categoriaPaiId; }
    public void setCategoriaPaiId(UUID categoriaPaiId) { this.categoriaPaiId = categoriaPaiId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
