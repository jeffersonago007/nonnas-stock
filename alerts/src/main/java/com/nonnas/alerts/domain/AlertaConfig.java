package com.nonnas.alerts.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Configuração de alerta com escopo flexível: {@code insumoId} e {@code filialId}
 * podem ser nulos para indicar "todos". O algoritmo de match em
 * {@code AvaliadorAlertasService} prioriza configs mais específicas.
 *
 * <p>Semântica do {@code threshold} varia por {@link TipoAlerta}:
 * <ul>
 *   <li>ESTOQUE_MINIMO_PERCENTUAL — % entre 0 e 100 (exclusive 0)</li>
 *   <li>ESTOQUE_MINIMO_ABSOLUTO — quantidade na unidade base &gt; 0</li>
 *   <li>VENCIMENTO_PROXIMO_DIAS — dias inteiros &gt; 0</li>
 *   <li>RUPTURA — não aplicável (threshold = null)</li>
 * </ul>
 */
public final class AlertaConfig {

    private final AlertaConfigId id;
    private final TipoAlerta tipo;
    private final UUID insumoId;
    private final UUID filialId;
    private BigDecimal threshold;
    private boolean ativo;
    private int prioridade;
    private String observacao;
    private final Instant createdAt;
    private Instant updatedAt;

    public AlertaConfig(AlertaConfigId id, TipoAlerta tipo, UUID insumoId, UUID filialId,
                        BigDecimal threshold, boolean ativo, int prioridade, String observacao,
                        Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.tipo = Objects.requireNonNull(tipo);
        this.insumoId = insumoId;
        this.filialId = filialId;
        validarThreshold(tipo, threshold);
        this.threshold = threshold;
        this.ativo = ativo;
        this.prioridade = prioridade;
        this.observacao = observacao;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static AlertaConfig novo(TipoAlerta tipo, UUID insumoId, UUID filialId,
                                    BigDecimal threshold, int prioridade, String observacao,
                                    Instant agora) {
        return new AlertaConfig(AlertaConfigId.generate(), tipo, insumoId, filialId,
                threshold, true, prioridade, observacao, agora, agora);
    }

    public void atualizar(BigDecimal novoThreshold, int novaPrioridade,
                          String novaObservacao, Instant agora) {
        validarThreshold(this.tipo, novoThreshold);
        this.threshold = novoThreshold;
        this.prioridade = novaPrioridade;
        this.observacao = novaObservacao;
        this.updatedAt = agora;
    }

    public void ativar(Instant agora) { this.ativo = true; this.updatedAt = agora; }
    public void desativar(Instant agora) { this.ativo = false; this.updatedAt = agora; }

    /** Quão específica é esta config? Mais alto = mais específico. */
    public int especificidade() {
        int s = 0;
        if (insumoId != null) s += 2;
        if (filialId != null) s += 1;
        return s;
    }

    private static void validarThreshold(TipoAlerta tipo, BigDecimal threshold) {
        if (tipo == TipoAlerta.RUPTURA) {
            if (threshold != null) {
                throw new ValidationException("RUPTURA não aceita threshold");
            }
            return;
        }
        if (threshold == null || threshold.signum() <= 0) {
            throw new ValidationException("Threshold deve ser positivo para o tipo " + tipo);
        }
        if (tipo == TipoAlerta.ESTOQUE_MINIMO_PERCENTUAL
                && threshold.compareTo(new BigDecimal("100")) > 0) {
            throw new ValidationException("Threshold percentual não pode exceder 100");
        }
        if (tipo == TipoAlerta.VENCIMENTO_PROXIMO_DIAS
                && threshold.scale() > 0 && threshold.stripTrailingZeros().scale() > 0) {
            throw new ValidationException("Dias até vencimento deve ser inteiro");
        }
    }

    public AlertaConfigId id() { return id; }
    public TipoAlerta tipo() { return tipo; }
    public Optional<UUID> insumoIdOpt() { return Optional.ofNullable(insumoId); }
    public Optional<UUID> filialIdOpt() { return Optional.ofNullable(filialId); }
    public Optional<BigDecimal> thresholdOpt() { return Optional.ofNullable(threshold); }
    public boolean ativo() { return ativo; }
    public int prioridade() { return prioridade; }
    public Optional<String> observacaoOpt() { return Optional.ofNullable(observacao); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
