package com.nonnas.operations.infrastructure.persistence;

import com.nonnas.operations.domain.StatusTransferencia;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transferencias")
public class TransferenciaEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "filial_origem_id", nullable = false, updatable = false) private UUID filialOrigemId;
    @Column(name = "filial_destino_id", nullable = false, updatable = false) private UUID filialDestinoId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatusTransferencia status;
    @Column(name = "solicitado_por", nullable = false, updatable = false) private UUID solicitadoPor;
    @Column(name = "aprovado_por") private UUID aprovadoPor;
    @Column(name = "enviado_por") private UUID enviadoPor;
    @Column(name = "recebido_por") private UUID recebidoPor;
    @Column(name = "data_solicitacao", nullable = false, updatable = false) private Instant dataSolicitacao;
    @Column(name = "data_aprovacao") private Instant dataAprovacao;
    @Column(name = "data_envio") private Instant dataEnvio;
    @Column(name = "data_recebimento") private Instant dataRecebimento;
    @Column private String observacao;
    @Column(name = "mov_saida_id") private UUID movSaidaId;
    @Column(name = "mov_entrada_id") private UUID movEntradaId;
    @Column(name = "motivo_cancelamento") private String motivoCancelamento;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "transferencia_id", nullable = false, updatable = false)
    private List<ItemTransferenciaEntity> itens = new ArrayList<>();

    public TransferenciaEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getFilialOrigemId() { return filialOrigemId; } public void setFilialOrigemId(UUID v) { this.filialOrigemId = v; }
    public UUID getFilialDestinoId() { return filialDestinoId; } public void setFilialDestinoId(UUID v) { this.filialDestinoId = v; }
    public StatusTransferencia getStatus() { return status; } public void setStatus(StatusTransferencia v) { this.status = v; }
    public UUID getSolicitadoPor() { return solicitadoPor; } public void setSolicitadoPor(UUID v) { this.solicitadoPor = v; }
    public UUID getAprovadoPor() { return aprovadoPor; } public void setAprovadoPor(UUID v) { this.aprovadoPor = v; }
    public UUID getEnviadoPor() { return enviadoPor; } public void setEnviadoPor(UUID v) { this.enviadoPor = v; }
    public UUID getRecebidoPor() { return recebidoPor; } public void setRecebidoPor(UUID v) { this.recebidoPor = v; }
    public Instant getDataSolicitacao() { return dataSolicitacao; } public void setDataSolicitacao(Instant v) { this.dataSolicitacao = v; }
    public Instant getDataAprovacao() { return dataAprovacao; } public void setDataAprovacao(Instant v) { this.dataAprovacao = v; }
    public Instant getDataEnvio() { return dataEnvio; } public void setDataEnvio(Instant v) { this.dataEnvio = v; }
    public Instant getDataRecebimento() { return dataRecebimento; } public void setDataRecebimento(Instant v) { this.dataRecebimento = v; }
    public String getObservacao() { return observacao; } public void setObservacao(String v) { this.observacao = v; }
    public UUID getMovSaidaId() { return movSaidaId; } public void setMovSaidaId(UUID v) { this.movSaidaId = v; }
    public UUID getMovEntradaId() { return movEntradaId; } public void setMovEntradaId(UUID v) { this.movEntradaId = v; }
    public String getMotivoCancelamento() { return motivoCancelamento; } public void setMotivoCancelamento(String v) { this.motivoCancelamento = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }
    public List<ItemTransferenciaEntity> getItens() { return itens; } public void setItens(List<ItemTransferenciaEntity> v) { this.itens = v; }
}
