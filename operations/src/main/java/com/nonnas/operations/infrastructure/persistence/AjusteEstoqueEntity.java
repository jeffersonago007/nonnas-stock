package com.nonnas.operations.infrastructure.persistence;

import com.nonnas.operations.domain.StatusAjuste;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ajustes_estoque")
public class AjusteEstoqueEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "filial_id", nullable = false, updatable = false) private UUID filialId;
    @Column(name = "insumo_id", nullable = false, updatable = false) private UUID insumoId;
    @Column(name = "unidade_id", nullable = false, updatable = false) private UUID unidadeId;
    @Column(name = "quantidade_diff", nullable = false, precision = 20, scale = 4, updatable = false) private BigDecimal quantidadeDiff;
    @Column(nullable = false, updatable = false) private String motivo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25) private StatusAjuste status;
    @Column(name = "requer_aprovacao", nullable = false, updatable = false) private boolean requerAprovacao;
    @Column(name = "solicitado_por", nullable = false, updatable = false) private UUID solicitadoPor;
    @Column(name = "aprovado_por") private UUID aprovadoPor;
    @Column(name = "data_solicitacao", nullable = false, updatable = false) private Instant dataSolicitacao;
    @Column(name = "data_aprovacao") private Instant dataAprovacao;
    @Column(name = "mov_id") private UUID movId;
    @Column(name = "origem_transferencia_id", updatable = false) private UUID origemTransferenciaId;
    @Column(name = "rejeicao_motivo") private String rejeicaoMotivo;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public AjusteEstoqueEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getFilialId() { return filialId; } public void setFilialId(UUID v) { this.filialId = v; }
    public UUID getInsumoId() { return insumoId; } public void setInsumoId(UUID v) { this.insumoId = v; }
    public UUID getUnidadeId() { return unidadeId; } public void setUnidadeId(UUID v) { this.unidadeId = v; }
    public BigDecimal getQuantidadeDiff() { return quantidadeDiff; } public void setQuantidadeDiff(BigDecimal v) { this.quantidadeDiff = v; }
    public String getMotivo() { return motivo; } public void setMotivo(String v) { this.motivo = v; }
    public StatusAjuste getStatus() { return status; } public void setStatus(StatusAjuste v) { this.status = v; }
    public boolean isRequerAprovacao() { return requerAprovacao; } public void setRequerAprovacao(boolean v) { this.requerAprovacao = v; }
    public UUID getSolicitadoPor() { return solicitadoPor; } public void setSolicitadoPor(UUID v) { this.solicitadoPor = v; }
    public UUID getAprovadoPor() { return aprovadoPor; } public void setAprovadoPor(UUID v) { this.aprovadoPor = v; }
    public Instant getDataSolicitacao() { return dataSolicitacao; } public void setDataSolicitacao(Instant v) { this.dataSolicitacao = v; }
    public Instant getDataAprovacao() { return dataAprovacao; } public void setDataAprovacao(Instant v) { this.dataAprovacao = v; }
    public UUID getMovId() { return movId; } public void setMovId(UUID v) { this.movId = v; }
    public UUID getOrigemTransferenciaId() { return origemTransferenciaId; } public void setOrigemTransferenciaId(UUID v) { this.origemTransferenciaId = v; }
    public String getRejeicaoMotivo() { return rejeicaoMotivo; } public void setRejeicaoMotivo(String v) { this.rejeicaoMotivo = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
