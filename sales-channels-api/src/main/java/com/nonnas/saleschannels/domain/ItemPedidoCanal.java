package com.nonnas.saleschannels.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Item de um pedido recebido por canal externo, em forma canônica.
 *
 * <p>{@code externalCode} é o código que o canal envia (ex.: SKU iFood,
 * `merchantSku` Open Delivery) — a resolução para um {@code ProdutoVendavel}
 * acontece em T-CANAL-04 ({@code produtoVendavelId} fica nulo até lá).
 *
 * <p>{@code preco} é o preço total da linha (quantidade × unitário), já
 * computado na origem para evitar arredondamento divergente.
 */
public final class ItemPedidoCanal {

    private final UUID id;
    private final int sequencia;
    private final String externalCode;
    private final String nome;
    private final BigDecimal quantidade;
    private final String unidade;
    private final BigDecimal precoUnitario;
    private final BigDecimal precoTotal;
    private final String observacao;
    private UUID produtoVendavelId;

    public ItemPedidoCanal(UUID id, int sequencia, String externalCode, String nome,
                           BigDecimal quantidade, String unidade,
                           BigDecimal precoUnitario, BigDecimal precoTotal,
                           String observacao, UUID produtoVendavelId) {
        this.id = Objects.requireNonNull(id);
        if (sequencia <= 0) {
            throw new ValidationException("sequência deve ser positiva");
        }
        this.sequencia = sequencia;
        this.externalCode = externalCode;
        this.nome = exigir(nome, "nome");
        this.quantidade = exigirPositivo(quantidade, "quantidade");
        this.unidade = exigir(unidade, "unidade");
        this.precoUnitario = exigirNaoNegativo(precoUnitario, "precoUnitario");
        this.precoTotal = exigirNaoNegativo(precoTotal, "precoTotal");
        this.observacao = observacao;
        this.produtoVendavelId = produtoVendavelId;
    }

    public static ItemPedidoCanal novo(int sequencia, String externalCode, String nome,
                                       BigDecimal quantidade, String unidade,
                                       BigDecimal precoUnitario, BigDecimal precoTotal,
                                       String observacao) {
        return new ItemPedidoCanal(UUID.randomUUID(), sequencia, externalCode, nome,
                quantidade, unidade, precoUnitario, precoTotal, observacao, null);
    }

    /** Marca este item como resolvido para um produto vendável (T-CANAL-04). */
    public void resolverProdutoVendavel(UUID produtoVendavelId) {
        this.produtoVendavelId = Objects.requireNonNull(produtoVendavelId);
    }

    private static String exigir(String v, String campo) {
        if (v == null || v.isBlank()) {
            throw new ValidationException(campo + " é obrigatório");
        }
        return v;
    }

    private static BigDecimal exigirPositivo(BigDecimal v, String campo) {
        if (v == null || v.signum() <= 0) {
            throw new ValidationException(campo + " deve ser positivo");
        }
        return v;
    }

    private static BigDecimal exigirNaoNegativo(BigDecimal v, String campo) {
        if (v == null || v.signum() < 0) {
            throw new ValidationException(campo + " não pode ser negativo");
        }
        return v;
    }

    public UUID id() { return id; }
    public int sequencia() { return sequencia; }
    public Optional<String> externalCodeOpt() { return Optional.ofNullable(externalCode); }
    public String nome() { return nome; }
    public BigDecimal quantidade() { return quantidade; }
    public String unidade() { return unidade; }
    public BigDecimal precoUnitario() { return precoUnitario; }
    public BigDecimal precoTotal() { return precoTotal; }
    public Optional<String> observacaoOpt() { return Optional.ofNullable(observacao); }
    public Optional<UUID> produtoVendavelIdOpt() { return Optional.ofNullable(produtoVendavelId); }
}
