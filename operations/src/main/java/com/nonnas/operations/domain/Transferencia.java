package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Transferência entre filiais com state machine
 * SOLICITADA → APROVADA → EM_TRANSITO → RECEBIDA / CANCELADA.
 *
 * <p>Cada transição é um método de domínio que valida o estado corrente e
 * grava timestamps + autor da ação. Use cases orquestram efeitos colaterais
 * (criar movimentações, gerar ajustes por divergência).
 */
public final class Transferencia {

    private final TransferenciaId id;
    private final UUID filialOrigemId;
    private final UUID filialDestinoId;
    private StatusTransferencia status;
    private final UUID solicitadoPor;
    private UUID aprovadoPor;
    private UUID enviadoPor;
    private UUID recebidoPor;
    private final Instant dataSolicitacao;
    private Instant dataAprovacao;
    private Instant dataEnvio;
    private Instant dataRecebimento;
    private String observacao;
    private UUID movSaidaId;
    private UUID movEntradaId;
    private String motivoCancelamento;
    private final List<ItemTransferencia> itens;
    private final Instant createdAt;
    private Instant updatedAt;

    public Transferencia(TransferenciaId id, UUID filialOrigemId, UUID filialDestinoId,
                         StatusTransferencia status, UUID solicitadoPor, UUID aprovadoPor,
                         UUID enviadoPor, UUID recebidoPor,
                         Instant dataSolicitacao, Instant dataAprovacao, Instant dataEnvio,
                         Instant dataRecebimento, String observacao,
                         UUID movSaidaId, UUID movEntradaId, String motivoCancelamento,
                         List<ItemTransferencia> itens, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.filialOrigemId = Objects.requireNonNull(filialOrigemId);
        this.filialDestinoId = Objects.requireNonNull(filialDestinoId);
        if (filialOrigemId.equals(filialDestinoId)) {
            throw new ValidationException("Filial de origem e destino devem ser distintas");
        }
        this.status = Objects.requireNonNull(status);
        this.solicitadoPor = Objects.requireNonNull(solicitadoPor);
        this.aprovadoPor = aprovadoPor;
        this.enviadoPor = enviadoPor;
        this.recebidoPor = recebidoPor;
        this.dataSolicitacao = Objects.requireNonNull(dataSolicitacao);
        this.dataAprovacao = dataAprovacao;
        this.dataEnvio = dataEnvio;
        this.dataRecebimento = dataRecebimento;
        this.observacao = observacao;
        this.movSaidaId = movSaidaId;
        this.movEntradaId = movEntradaId;
        this.motivoCancelamento = motivoCancelamento;
        this.itens = new ArrayList<>(validarItens(itens));
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Transferencia solicitar(UUID filialOrigemId, UUID filialDestinoId, UUID solicitadoPor,
                                          List<ItemTransferencia> itens, String observacao, Instant agora) {
        return new Transferencia(
                TransferenciaId.generate(), filialOrigemId, filialDestinoId,
                StatusTransferencia.SOLICITADA, solicitadoPor, null, null, null,
                agora, null, null, null, observacao, null, null, null,
                itens, agora, agora);
    }

    public void aprovar(UUID usuarioId, Instant agora) {
        if (!status.podeAprovar()) {
            throw new BusinessRuleException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Transferência não pode ser aprovada no estado " + status);
        }
        this.status = StatusTransferencia.APROVADA;
        this.aprovadoPor = Objects.requireNonNull(usuarioId);
        this.dataAprovacao = agora;
        this.updatedAt = agora;
    }

    public void registrarEnvio(UUID usuarioId, UUID movimentacaoSaidaId, Instant agora) {
        if (!status.podeEnviar()) {
            throw new BusinessRuleException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Transferência não pode ser enviada no estado " + status);
        }
        this.status = StatusTransferencia.EM_TRANSITO;
        this.enviadoPor = Objects.requireNonNull(usuarioId);
        this.movSaidaId = Objects.requireNonNull(movimentacaoSaidaId);
        this.dataEnvio = agora;
        this.updatedAt = agora;
    }

    /**
     * Registra recebimento. Para cada item, espera entrada em {@code quantidadesRecebidas}
     * (chave: {@code itemId}). Valor faltante = item não recebido (qtd_recebida = 0).
     */
    public void registrarRecebimento(UUID usuarioId, Map<UUID, BigDecimal> quantidadesRecebidas,
                                     UUID movimentacaoEntradaId, Instant agora) {
        if (!status.podeReceber()) {
            throw new BusinessRuleException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Transferência não pode ser recebida no estado " + status);
        }
        for (ItemTransferencia it : itens) {
            BigDecimal qtdRecebida = quantidadesRecebidas.getOrDefault(it.id(), BigDecimal.ZERO);
            it.registrarRecebimento(qtdRecebida);
        }
        this.status = StatusTransferencia.RECEBIDA;
        this.recebidoPor = Objects.requireNonNull(usuarioId);
        this.movEntradaId = Objects.requireNonNull(movimentacaoEntradaId);
        this.dataRecebimento = agora;
        this.updatedAt = agora;
    }

    public void cancelar(String motivo, Instant agora) {
        if (!status.podeCancelar()) {
            throw new BusinessRuleException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Transferência não pode ser cancelada no estado " + status);
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ValidationException("Motivo do cancelamento é obrigatório");
        }
        this.status = StatusTransferencia.CANCELADA;
        this.motivoCancelamento = motivo.trim();
        this.updatedAt = agora;
    }

    public List<ItemTransferencia> itensComDivergencia() {
        return itens.stream().filter(ItemTransferencia::temDivergencia).toList();
    }

    private static List<ItemTransferencia> validarItens(List<ItemTransferencia> itens) {
        Objects.requireNonNull(itens, "itens");
        if (itens.isEmpty()) {
            throw new ValidationException("Transferência deve ter ao menos um item");
        }
        var insumosVistos = new HashSet<UUID>();
        for (var i : itens) {
            if (!insumosVistos.add(i.insumoId())) {
                throw new ValidationException("Insumo duplicado na transferência: " + i.insumoId());
            }
        }
        return itens;
    }

    public TransferenciaId id() { return id; }
    public UUID filialOrigemId() { return filialOrigemId; }
    public UUID filialDestinoId() { return filialDestinoId; }
    public StatusTransferencia status() { return status; }
    public UUID solicitadoPor() { return solicitadoPor; }
    public Optional<UUID> aprovadoPorOpt() { return Optional.ofNullable(aprovadoPor); }
    public Optional<UUID> enviadoPorOpt() { return Optional.ofNullable(enviadoPor); }
    public Optional<UUID> recebidoPorOpt() { return Optional.ofNullable(recebidoPor); }
    public Instant dataSolicitacao() { return dataSolicitacao; }
    public Optional<Instant> dataAprovacaoOpt() { return Optional.ofNullable(dataAprovacao); }
    public Optional<Instant> dataEnvioOpt() { return Optional.ofNullable(dataEnvio); }
    public Optional<Instant> dataRecebimentoOpt() { return Optional.ofNullable(dataRecebimento); }
    public Optional<String> observacaoOpt() { return Optional.ofNullable(observacao); }
    public Optional<UUID> movSaidaIdOpt() { return Optional.ofNullable(movSaidaId); }
    public Optional<UUID> movEntradaIdOpt() { return Optional.ofNullable(movEntradaId); }
    public Optional<String> motivoCancelamentoOpt() { return Optional.ofNullable(motivoCancelamento); }
    public List<ItemTransferencia> itens() { return Collections.unmodifiableList(itens); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
