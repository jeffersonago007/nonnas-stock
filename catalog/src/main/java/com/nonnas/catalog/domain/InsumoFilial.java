package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Configuração específica de um insumo numa filial: estoque mínimo,
 * estoque máximo, ponto de pedido. {@code filialId} é um UUID porque a
 * entidade {@code Filial} vive no módulo identity — referenciamos por
 * UUID para não criar dependência cruzada entre módulos. A FK física
 * para {@code filiais} é adicionada em T09 quando o app/ assembly unifica
 * as migrations.
 */
public final class InsumoFilial {

    private final InsumoFilialId id;
    private final InsumoId insumoId;
    private final UUID filialId;
    private BigDecimal estoqueMinimo;
    private BigDecimal estoqueMaximo;
    private BigDecimal pontoPedido;
    private boolean ativo;
    private final Instant createdAt;
    private Instant updatedAt;

    public InsumoFilial(InsumoFilialId id, InsumoId insumoId, UUID filialId,
                        BigDecimal estoqueMinimo, BigDecimal estoqueMaximo, BigDecimal pontoPedido,
                        boolean ativo, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.insumoId = Objects.requireNonNull(insumoId, "insumoId");
        this.filialId = Objects.requireNonNull(filialId, "filialId");
        this.estoqueMinimo = validarMinimo(estoqueMinimo);
        this.estoqueMaximo = estoqueMaximo;
        this.pontoPedido = pontoPedido;
        this.ativo = ativo;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static InsumoFilial novo(InsumoId insumoId, UUID filialId,
                                    BigDecimal estoqueMinimo, BigDecimal estoqueMaximo,
                                    BigDecimal pontoPedido, Instant agora) {
        return new InsumoFilial(InsumoFilialId.generate(), insumoId, filialId,
                estoqueMinimo, estoqueMaximo, pontoPedido, true, agora, agora);
    }

    public void atualizarParametros(BigDecimal estoqueMinimo, BigDecimal estoqueMaximo,
                                    BigDecimal pontoPedido, Instant agora) {
        this.estoqueMinimo = validarMinimo(estoqueMinimo);
        this.estoqueMaximo = estoqueMaximo;
        this.pontoPedido = pontoPedido;
        this.updatedAt = agora;
    }

    public void desativar(Instant agora) { this.ativo = false; this.updatedAt = agora; }
    public void ativar(Instant agora) { this.ativo = true; this.updatedAt = agora; }

    private static BigDecimal validarMinimo(BigDecimal valor) {
        if (valor == null) return BigDecimal.ZERO;
        if (valor.signum() < 0) {
            throw new ValidationException("Estoque mínimo não pode ser negativo");
        }
        return valor;
    }

    public InsumoFilialId id() { return id; }
    public InsumoId insumoId() { return insumoId; }
    public UUID filialId() { return filialId; }
    public BigDecimal estoqueMinimo() { return estoqueMinimo; }
    public Optional<BigDecimal> estoqueMaximo() { return Optional.ofNullable(estoqueMaximo); }
    public Optional<BigDecimal> pontoPedido() { return Optional.ofNullable(pontoPedido); }
    public boolean ativo() { return ativo; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
