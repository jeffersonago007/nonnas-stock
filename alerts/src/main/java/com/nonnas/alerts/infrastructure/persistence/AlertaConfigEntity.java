package com.nonnas.alerts.infrastructure.persistence;

import com.nonnas.alerts.domain.TipoAlerta;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alertas_config")
public class AlertaConfigEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40, updatable = false) private TipoAlerta tipo;
    @Column(name = "insumo_id", updatable = false) private UUID insumoId;
    @Column(name = "filial_id", updatable = false) private UUID filialId;
    @Column(precision = 20, scale = 4) private BigDecimal threshold;
    @Column(nullable = false) private boolean ativo;
    @Column(nullable = false) private int prioridade;
    @Column private String observacao;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public AlertaConfigEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public TipoAlerta getTipo() { return tipo; } public void setTipo(TipoAlerta v) { this.tipo = v; }
    public UUID getInsumoId() { return insumoId; } public void setInsumoId(UUID v) { this.insumoId = v; }
    public UUID getFilialId() { return filialId; } public void setFilialId(UUID v) { this.filialId = v; }
    public BigDecimal getThreshold() { return threshold; } public void setThreshold(BigDecimal v) { this.threshold = v; }
    public boolean isAtivo() { return ativo; } public void setAtivo(boolean v) { this.ativo = v; }
    public int getPrioridade() { return prioridade; } public void setPrioridade(int v) { this.prioridade = v; }
    public String getObservacao() { return observacao; } public void setObservacao(String v) { this.observacao = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
