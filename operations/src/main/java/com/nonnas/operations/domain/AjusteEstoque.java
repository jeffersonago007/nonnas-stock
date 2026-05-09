package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Ajuste manual de estoque. Diferença pode ser positiva (entrada de ajuste)
 * ou negativa (saída de ajuste). Se {@code |quantidade_diff| > threshold},
 * fica {@code PENDENTE_APROVACAO} e não gera movimentação até aprovação;
 * caso contrário, é criado já como {@code APROVADO} e o use case dispara
 * a movimentação imediatamente.
 */
public final class AjusteEstoque {

    private final AjusteEstoqueId id;
    private final UUID filialId;
    private final UUID insumoId;
    private final UUID unidadeId;
    private final BigDecimal quantidadeDiff;
    private final String motivo;
    private StatusAjuste status;
    private final boolean requerAprovacao;
    private final UUID solicitadoPor;
    private UUID aprovadoPor;
    private final Instant dataSolicitacao;
    private Instant dataAprovacao;
    private UUID movId;
    private final UUID origemTransferenciaId;
    private String rejeicaoMotivo;
    private final Instant createdAt;
    private Instant updatedAt;

    public AjusteEstoque(AjusteEstoqueId id, UUID filialId, UUID insumoId, UUID unidadeId,
                         BigDecimal quantidadeDiff, String motivo, StatusAjuste status,
                         boolean requerAprovacao, UUID solicitadoPor, UUID aprovadoPor,
                         Instant dataSolicitacao, Instant dataAprovacao,
                         UUID movId, UUID origemTransferenciaId, String rejeicaoMotivo,
                         Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.filialId = Objects.requireNonNull(filialId);
        this.insumoId = Objects.requireNonNull(insumoId);
        this.unidadeId = Objects.requireNonNull(unidadeId);
        this.quantidadeDiff = Objects.requireNonNull(quantidadeDiff);
        if (quantidadeDiff.signum() == 0) {
            throw new ValidationException("Quantidade de ajuste não pode ser zero");
        }
        this.motivo = validarMotivo(motivo);
        this.status = Objects.requireNonNull(status);
        this.requerAprovacao = requerAprovacao;
        this.solicitadoPor = Objects.requireNonNull(solicitadoPor);
        this.aprovadoPor = aprovadoPor;
        this.dataSolicitacao = Objects.requireNonNull(dataSolicitacao);
        this.dataAprovacao = dataAprovacao;
        this.movId = movId;
        this.origemTransferenciaId = origemTransferenciaId;
        this.rejeicaoMotivo = rejeicaoMotivo;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    /**
     * @param thresholdAprovacao se {@code |quantidade_diff| > thresholdAprovacao}
     *                          o ajuste fica pendente até aprovação manual.
     */
    public static AjusteEstoque novo(UUID filialId, UUID insumoId, UUID unidadeId,
                                     BigDecimal quantidadeDiff, String motivo, UUID solicitadoPor,
                                     BigDecimal thresholdAprovacao, UUID origemTransferenciaId,
                                     Instant agora) {
        boolean requer = quantidadeDiff.abs().compareTo(thresholdAprovacao) > 0;
        StatusAjuste statusInicial = requer ? StatusAjuste.PENDENTE_APROVACAO : StatusAjuste.APROVADO;
        Instant dataAprov = requer ? null : agora;
        return new AjusteEstoque(
                AjusteEstoqueId.generate(), filialId, insumoId, unidadeId,
                quantidadeDiff, motivo, statusInicial, requer,
                solicitadoPor, requer ? null : solicitadoPor,
                agora, dataAprov, null, origemTransferenciaId, null,
                agora, agora);
    }

    public void aprovar(UUID usuarioId, UUID movimentacaoId, Instant agora) {
        if (!status.podeAprovar()) {
            throw new BusinessRuleException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Ajuste não pode ser aprovado no estado " + status);
        }
        this.status = StatusAjuste.APROVADO;
        this.aprovadoPor = Objects.requireNonNull(usuarioId);
        this.movId = Objects.requireNonNull(movimentacaoId);
        this.dataAprovacao = agora;
        this.updatedAt = agora;
    }

    public void rejeitar(UUID usuarioId, String motivoRejeicao, Instant agora) {
        if (!status.podeRejeitar()) {
            throw new BusinessRuleException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Ajuste não pode ser rejeitado no estado " + status);
        }
        if (motivoRejeicao == null || motivoRejeicao.isBlank()) {
            throw new ValidationException("Motivo da rejeição é obrigatório");
        }
        this.status = StatusAjuste.REJEITADO;
        this.aprovadoPor = Objects.requireNonNull(usuarioId);
        this.rejeicaoMotivo = motivoRejeicao.trim();
        this.dataAprovacao = agora;
        this.updatedAt = agora;
    }

    /** Após criação direta como APROVADO (sem aprovação manual), grava o id da movimentação. */
    public void anexarMovimentacao(UUID movimentacaoId, Instant agora) {
        if (status != StatusAjuste.APROVADO) {
            throw new IllegalStateException("Apenas ajustes APROVADOS podem anexar movimentação");
        }
        if (this.movId != null) {
            throw new IllegalStateException("Movimentação já anexada");
        }
        this.movId = Objects.requireNonNull(movimentacaoId);
        this.updatedAt = agora;
    }

    private static String validarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ValidationException("Motivo do ajuste é obrigatório");
        }
        String m = motivo.trim();
        if (m.length() > 500) {
            throw new ValidationException("Motivo do ajuste não pode exceder 500 caracteres");
        }
        return m;
    }

    public AjusteEstoqueId id() { return id; }
    public UUID filialId() { return filialId; }
    public UUID insumoId() { return insumoId; }
    public UUID unidadeId() { return unidadeId; }
    public BigDecimal quantidadeDiff() { return quantidadeDiff; }
    public String motivo() { return motivo; }
    public StatusAjuste status() { return status; }
    public boolean requerAprovacao() { return requerAprovacao; }
    public UUID solicitadoPor() { return solicitadoPor; }
    public Optional<UUID> aprovadoPorOpt() { return Optional.ofNullable(aprovadoPor); }
    public Instant dataSolicitacao() { return dataSolicitacao; }
    public Optional<Instant> dataAprovacaoOpt() { return Optional.ofNullable(dataAprovacao); }
    public Optional<UUID> movIdOpt() { return Optional.ofNullable(movId); }
    public Optional<UUID> origemTransferenciaIdOpt() { return Optional.ofNullable(origemTransferenciaId); }
    public Optional<String> rejeicaoMotivoOpt() { return Optional.ofNullable(rejeicaoMotivo); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
