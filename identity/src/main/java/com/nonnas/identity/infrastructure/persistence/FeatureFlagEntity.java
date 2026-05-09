package com.nonnas.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "feature_flags")
public class FeatureFlagEntity {

    @Id
    @Column(name = "chave", length = 64)
    private String chave;

    @Column(name = "descricao", nullable = false, columnDefinition = "text")
    private String descricao;

    @Column(name = "habilitada", nullable = false)
    private boolean habilitada;

    @Column(name = "rollout_pct", nullable = false)
    private int rolloutPct;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    public FeatureFlagEntity() {}

    public String getChave() { return chave; }
    public String getDescricao() { return descricao; }
    public boolean isHabilitada() { return habilitada; }
    public int getRolloutPct() { return rolloutPct; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getAtualizadaEm() { return atualizadaEm; }
}
