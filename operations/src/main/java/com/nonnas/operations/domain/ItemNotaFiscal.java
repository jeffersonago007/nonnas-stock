package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Item persistido de uma {@link NotaFiscal}. Reflete uma linha da nota
 * fiscal já vinculada a um insumo do catálogo (após resolução por de-para
 * ou criação automática).
 */
public final class ItemNotaFiscal {

    private final UUID id;
    private final UUID insumoId;
    private final String codigoFornecedor;
    private final String descricaoOrigem;
    private final BigDecimal quantidade;
    private final UUID unidadeMedidaId;
    private final BigDecimal valorUnitario;
    private final BigDecimal valorTotal;
    private final String lote;
    private final LocalDate dataValidade;

    public ItemNotaFiscal(UUID id, UUID insumoId, String codigoFornecedor, String descricaoOrigem,
                          BigDecimal quantidade, UUID unidadeMedidaId, BigDecimal valorUnitario,
                          BigDecimal valorTotal, String lote, LocalDate dataValidade) {
        this.id = Objects.requireNonNull(id, "id");
        this.insumoId = Objects.requireNonNull(insumoId, "insumoId");
        this.codigoFornecedor = codigoFornecedor;
        this.descricaoOrigem = Objects.requireNonNull(descricaoOrigem, "descricaoOrigem");
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new ValidationException("Quantidade deve ser positiva");
        }
        this.quantidade = quantidade;
        this.unidadeMedidaId = Objects.requireNonNull(unidadeMedidaId, "unidadeMedidaId");
        if (valorUnitario == null || valorUnitario.signum() < 0) {
            throw new ValidationException("Valor unitário não pode ser negativo");
        }
        this.valorUnitario = valorUnitario;
        if (valorTotal == null || valorTotal.signum() < 0) {
            throw new ValidationException("Valor total não pode ser negativo");
        }
        this.valorTotal = valorTotal;
        this.lote = lote;
        this.dataValidade = dataValidade;
    }

    public static ItemNotaFiscal novo(UUID insumoId, String codigoFornecedor, String descricaoOrigem,
                                      BigDecimal quantidade, UUID unidadeMedidaId,
                                      BigDecimal valorUnitario, BigDecimal valorTotal,
                                      String lote, LocalDate dataValidade) {
        return new ItemNotaFiscal(UUID.randomUUID(), insumoId, codigoFornecedor, descricaoOrigem,
                quantidade, unidadeMedidaId, valorUnitario, valorTotal, lote, dataValidade);
    }

    public UUID id() { return id; }
    public UUID insumoId() { return insumoId; }
    public Optional<String> codigoFornecedorOpt() { return Optional.ofNullable(codigoFornecedor); }
    public String descricaoOrigem() { return descricaoOrigem; }
    public BigDecimal quantidade() { return quantidade; }
    public UUID unidadeMedidaId() { return unidadeMedidaId; }
    public BigDecimal valorUnitario() { return valorUnitario; }
    public BigDecimal valorTotal() { return valorTotal; }
    public Optional<String> loteOpt() { return Optional.ofNullable(lote); }
    public Optional<LocalDate> dataValidadeOpt() { return Optional.ofNullable(dataValidade); }
}
