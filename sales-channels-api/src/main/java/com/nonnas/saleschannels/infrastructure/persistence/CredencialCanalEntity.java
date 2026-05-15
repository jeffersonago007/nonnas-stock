package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.domain.CanalTipo;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "canais_credenciais")
public class CredencialCanalEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Enumerated(EnumType.STRING) @Column(name = "canal_tipo", nullable = false, length = 40, updatable = false) private CanalTipo canalTipo;
    @Column(name = "filial_id", nullable = false, updatable = false) private UUID filialId;
    @Column(name = "merchant_externo_id", nullable = false, length = 120) private String merchantExternoId;
    @Column(name = "client_id", nullable = false, length = 200) private String clientId;
    @Column(name = "client_secret_cifrado", nullable = false, columnDefinition = "text") private String clientSecretCifrado;
    @Column(name = "base_url", length = 300) private String baseUrl;
    @Column(nullable = false) private boolean ativa;
    @Column(columnDefinition = "text") private String observacao;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public CredencialCanalEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public CanalTipo getCanalTipo() { return canalTipo; } public void setCanalTipo(CanalTipo v) { this.canalTipo = v; }
    public UUID getFilialId() { return filialId; } public void setFilialId(UUID v) { this.filialId = v; }
    public String getMerchantExternoId() { return merchantExternoId; } public void setMerchantExternoId(String v) { this.merchantExternoId = v; }
    public String getClientId() { return clientId; } public void setClientId(String v) { this.clientId = v; }
    public String getClientSecretCifrado() { return clientSecretCifrado; } public void setClientSecretCifrado(String v) { this.clientSecretCifrado = v; }
    public String getBaseUrl() { return baseUrl; } public void setBaseUrl(String v) { this.baseUrl = v; }
    public boolean isAtiva() { return ativa; } public void setAtiva(boolean v) { this.ativa = v; }
    public String getObservacao() { return observacao; } public void setObservacao(String v) { this.observacao = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
