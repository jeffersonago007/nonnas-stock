package com.nonnas.alerts.domain;

import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Registro de um disparo de alerta. {@code status} muda durante o ciclo de vida:
 * ATIVO → RESOLVIDO_AUTO (saldo voltou ao normal) ou RESOLVIDO_MANUAL (admin marcou).
 *
 * <p>{@code visualizadoEm} é ortogonal — um alerta pode ser visualizado pelo
 * usuário antes ou depois de ser resolvido, sem mudar status.
 */
public final class AlertaDisparado {

    private final AlertaDisparadoId id;
    private final AlertaConfigId configId;
    private final TipoAlerta tipo;
    private final UUID insumoId;
    private final UUID filialId;
    private final UUID loteId;
    private StatusAlerta status;
    private final BigDecimal saldoNoDisparo;
    private final String detalhe;
    private final Instant dataDisparo;
    private Instant dataResolucao;
    private Instant visualizadoEm;
    private UUID visualizadoPor;
    private UUID resolvidoPor;

    public AlertaDisparado(AlertaDisparadoId id, AlertaConfigId configId, TipoAlerta tipo,
                           UUID insumoId, UUID filialId, UUID loteId,
                           StatusAlerta status, BigDecimal saldoNoDisparo, String detalhe,
                           Instant dataDisparo, Instant dataResolucao,
                           Instant visualizadoEm, UUID visualizadoPor, UUID resolvidoPor) {
        this.id = Objects.requireNonNull(id);
        this.configId = Objects.requireNonNull(configId);
        this.tipo = Objects.requireNonNull(tipo);
        this.insumoId = Objects.requireNonNull(insumoId);
        this.filialId = Objects.requireNonNull(filialId);
        this.loteId = loteId;
        this.status = Objects.requireNonNull(status);
        this.saldoNoDisparo = saldoNoDisparo;
        this.detalhe = detalhe;
        this.dataDisparo = Objects.requireNonNull(dataDisparo);
        this.dataResolucao = dataResolucao;
        this.visualizadoEm = visualizadoEm;
        this.visualizadoPor = visualizadoPor;
        this.resolvidoPor = resolvidoPor;
    }

    public static AlertaDisparado disparar(AlertaConfigId configId, TipoAlerta tipo,
                                           UUID insumoId, UUID filialId, UUID loteId,
                                           BigDecimal saldoNoDisparo, String detalhe, Instant agora) {
        return new AlertaDisparado(AlertaDisparadoId.generate(), configId, tipo,
                insumoId, filialId, loteId, StatusAlerta.ATIVO,
                saldoNoDisparo, detalhe, agora, null, null, null, null);
    }

    public void resolverAuto(Instant agora) {
        if (!status.isAtivo()) {
            throw new BusinessRuleException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Alerta não está ativo: " + status);
        }
        this.status = StatusAlerta.RESOLVIDO_AUTO;
        this.dataResolucao = agora;
    }

    public void resolverManual(UUID usuarioId, Instant agora) {
        if (!status.isAtivo()) {
            throw new BusinessRuleException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Alerta não está ativo: " + status);
        }
        this.status = StatusAlerta.RESOLVIDO_MANUAL;
        this.resolvidoPor = Objects.requireNonNull(usuarioId);
        this.dataResolucao = agora;
    }

    public void marcarVisualizado(UUID usuarioId, Instant agora) {
        if (this.visualizadoEm != null) {
            throw new ValidationException("Alerta já foi visualizado");
        }
        this.visualizadoEm = agora;
        this.visualizadoPor = Objects.requireNonNull(usuarioId);
    }

    public AlertaDisparadoId id() { return id; }
    public AlertaConfigId configId() { return configId; }
    public TipoAlerta tipo() { return tipo; }
    public UUID insumoId() { return insumoId; }
    public UUID filialId() { return filialId; }
    public Optional<UUID> loteIdOpt() { return Optional.ofNullable(loteId); }
    public StatusAlerta status() { return status; }
    public Optional<BigDecimal> saldoNoDisparoOpt() { return Optional.ofNullable(saldoNoDisparo); }
    public Optional<String> detalheOpt() { return Optional.ofNullable(detalhe); }
    public Instant dataDisparo() { return dataDisparo; }
    public Optional<Instant> dataResolucaoOpt() { return Optional.ofNullable(dataResolucao); }
    public Optional<Instant> visualizadoEmOpt() { return Optional.ofNullable(visualizadoEm); }
    public Optional<UUID> visualizadoPorOpt() { return Optional.ofNullable(visualizadoPor); }
    public Optional<UUID> resolvidoPorOpt() { return Optional.ofNullable(resolvidoPor); }
}
