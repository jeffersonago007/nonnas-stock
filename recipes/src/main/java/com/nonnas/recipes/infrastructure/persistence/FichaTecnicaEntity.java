package com.nonnas.recipes.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fichas_tecnicas")
public class FichaTecnicaEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "produto_vendavel_id", nullable = false, updatable = false) private UUID produtoVendavelId;
    @Column(nullable = false, updatable = false) private int versao;
    @Column(name = "vigente_desde", nullable = false, updatable = false) private Instant vigenteDesde;
    @Column(name = "vigente_ate") private Instant vigenteAte;
    @Column(nullable = false) private boolean ativa;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ficha_tecnica_id", nullable = false, updatable = false)
    private List<ItemFichaTecnicaEntity> itens = new ArrayList<>();

    public FichaTecnicaEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getProdutoVendavelId() { return produtoVendavelId; } public void setProdutoVendavelId(UUID v) { this.produtoVendavelId = v; }
    public int getVersao() { return versao; } public void setVersao(int v) { this.versao = v; }
    public Instant getVigenteDesde() { return vigenteDesde; } public void setVigenteDesde(Instant v) { this.vigenteDesde = v; }
    public Instant getVigenteAte() { return vigenteAte; } public void setVigenteAte(Instant v) { this.vigenteAte = v; }
    public boolean isAtiva() { return ativa; } public void setAtiva(boolean v) { this.ativa = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }
    public List<ItemFichaTecnicaEntity> getItens() { return itens; } public void setItens(List<ItemFichaTecnicaEntity> v) { this.itens = v; }
}
