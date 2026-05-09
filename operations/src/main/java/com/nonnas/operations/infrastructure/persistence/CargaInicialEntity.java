package com.nonnas.operations.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cargas_iniciais")
public class CargaInicialEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "filial_id", nullable = false, updatable = false) private UUID filialId;
    @Column(name = "hash_planilha", nullable = false, length = 64, updatable = false) private String hashPlanilha;
    @Column(name = "nome_arquivo", nullable = false, updatable = false) private String nomeArquivo;
    @Column(name = "registros_processados", nullable = false, updatable = false) private int registrosProcessados;
    @Column(name = "registros_falhos", nullable = false, updatable = false) private int registrosFalhos;
    @Column(name = "solicitado_por", nullable = false, updatable = false) private UUID solicitadoPor;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public CargaInicialEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getFilialId() { return filialId; } public void setFilialId(UUID v) { this.filialId = v; }
    public String getHashPlanilha() { return hashPlanilha; } public void setHashPlanilha(String v) { this.hashPlanilha = v; }
    public String getNomeArquivo() { return nomeArquivo; } public void setNomeArquivo(String v) { this.nomeArquivo = v; }
    public int getRegistrosProcessados() { return registrosProcessados; } public void setRegistrosProcessados(int v) { this.registrosProcessados = v; }
    public int getRegistrosFalhos() { return registrosFalhos; } public void setRegistrosFalhos(int v) { this.registrosFalhos = v; }
    public UUID getSolicitadoPor() { return solicitadoPor; } public void setSolicitadoPor(UUID v) { this.solicitadoPor = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
}
