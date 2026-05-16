package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.domain.CanalTipo;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "canal_produto_depara")
public class CanalProdutoDeParaEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Enumerated(EnumType.STRING) @Column(name = "canal_tipo", nullable = false, length = 40, updatable = false) private CanalTipo canalTipo;
    @Column(name = "external_code", nullable = false, length = 120, updatable = false) private String externalCode;
    @Column(name = "filial_id", updatable = false) private UUID filialId;
    @Column(name = "produto_vendavel_id", nullable = false) private UUID produtoVendavelId;
    @Column(columnDefinition = "text") private String observacao;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public CanalProdutoDeParaEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public CanalTipo getCanalTipo() { return canalTipo; } public void setCanalTipo(CanalTipo v) { this.canalTipo = v; }
    public String getExternalCode() { return externalCode; } public void setExternalCode(String v) { this.externalCode = v; }
    public UUID getFilialId() { return filialId; } public void setFilialId(UUID v) { this.filialId = v; }
    public UUID getProdutoVendavelId() { return produtoVendavelId; } public void setProdutoVendavelId(UUID v) { this.produtoVendavelId = v; }
    public String getObservacao() { return observacao; } public void setObservacao(String v) { this.observacao = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
