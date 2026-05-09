package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Item de uma transferência. Mutável apenas no momento do recebimento —
 * {@code quantidadeRecebida} é fixada uma vez via {@link #registrarRecebimento}.
 * Tudo o mais é imutável após criação.
 */
public final class ItemTransferencia {

    private final UUID id;
    private final UUID insumoId;
    private final UUID unidadeId;
    private final BigDecimal quantidadeSolicitada;
    private BigDecimal quantidadeRecebida;

    public ItemTransferencia(UUID id, UUID insumoId, UUID unidadeId,
                             BigDecimal quantidadeSolicitada, BigDecimal quantidadeRecebida) {
        this.id = Objects.requireNonNull(id);
        this.insumoId = Objects.requireNonNull(insumoId);
        this.unidadeId = Objects.requireNonNull(unidadeId);
        this.quantidadeSolicitada = Objects.requireNonNull(quantidadeSolicitada);
        if (quantidadeSolicitada.signum() <= 0) {
            throw new ValidationException("Quantidade solicitada deve ser positiva");
        }
        if (quantidadeRecebida != null && quantidadeRecebida.signum() < 0) {
            throw new ValidationException("Quantidade recebida não pode ser negativa");
        }
        this.quantidadeRecebida = quantidadeRecebida;
    }

    public static ItemTransferencia novo(UUID insumoId, UUID unidadeId, BigDecimal quantidadeSolicitada) {
        return new ItemTransferencia(UUID.randomUUID(), insumoId, unidadeId, quantidadeSolicitada, null);
    }

    void registrarRecebimento(BigDecimal qtdRecebida) {
        if (this.quantidadeRecebida != null) {
            throw new ValidationException("Recebimento do item já foi registrado");
        }
        if (qtdRecebida == null || qtdRecebida.signum() < 0) {
            throw new ValidationException("Quantidade recebida não pode ser negativa");
        }
        this.quantidadeRecebida = qtdRecebida;
    }

    public BigDecimal divergencia() {
        if (quantidadeRecebida == null) {
            throw new IllegalStateException("Recebimento não registrado");
        }
        return quantidadeRecebida.subtract(quantidadeSolicitada);
    }

    public boolean temDivergencia() {
        return quantidadeRecebida != null && divergencia().signum() != 0;
    }

    public UUID id() { return id; }
    public UUID insumoId() { return insumoId; }
    public UUID unidadeId() { return unidadeId; }
    public BigDecimal quantidadeSolicitada() { return quantidadeSolicitada; }
    public Optional<BigDecimal> quantidadeRecebidaOpt() { return Optional.ofNullable(quantidadeRecebida); }
}
