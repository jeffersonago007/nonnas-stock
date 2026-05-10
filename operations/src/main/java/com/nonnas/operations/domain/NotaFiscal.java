package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Nota fiscal de entrada (modelo 55) lançada pelo operador. Aggregate root
 * que carrega os itens. Imutável após criação — alterações futuras devem
 * passar por estorno + nova nota.
 *
 * <p>{@code chaveNfe} é opcional: notas lançadas em modo manual (sem XML)
 * não têm chave SEFAZ. Quando presente, dispara o constraint UNIQUE no
 * banco que bloqueia reentrada da mesma nota.
 */
public final class NotaFiscal {

    private final NotaFiscalId id;
    private final UUID fornecedorId;
    private final UUID filialId;
    private final String numero;
    private final String serie;
    private final String chaveNfe;
    private final OffsetDateTime dataEmissao;
    private final Instant dataLancamento;
    private final BigDecimal valorTotal;
    private final String observacao;
    private final UUID createdByUsuarioId;
    private final UUID movimentacaoEntradaId;
    private final List<ItemNotaFiscal> itens;
    private final Instant createdAt;
    private final Instant updatedAt;

    public NotaFiscal(NotaFiscalId id, UUID fornecedorId, UUID filialId, String numero, String serie,
                      String chaveNfe, OffsetDateTime dataEmissao, Instant dataLancamento,
                      BigDecimal valorTotal, String observacao, UUID createdByUsuarioId,
                      UUID movimentacaoEntradaId, List<ItemNotaFiscal> itens,
                      Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.fornecedorId = Objects.requireNonNull(fornecedorId, "fornecedorId");
        this.filialId = Objects.requireNonNull(filialId, "filialId");
        this.numero = exigirNaoVazio(numero, "Número da nota é obrigatório");
        this.serie = exigirNaoVazio(serie, "Série da nota é obrigatória");
        this.chaveNfe = validarChave(chaveNfe);
        this.dataEmissao = Objects.requireNonNull(dataEmissao, "dataEmissao");
        this.dataLancamento = Objects.requireNonNull(dataLancamento, "dataLancamento");
        if (valorTotal == null || valorTotal.signum() < 0) {
            throw new ValidationException("Valor total não pode ser negativo");
        }
        this.valorTotal = valorTotal;
        this.observacao = observacao;
        this.createdByUsuarioId = Objects.requireNonNull(createdByUsuarioId, "createdByUsuarioId");
        this.movimentacaoEntradaId = Objects.requireNonNull(movimentacaoEntradaId, "movimentacaoEntradaId");
        Objects.requireNonNull(itens, "itens");
        if (itens.isEmpty()) {
            throw new ValidationException("Nota fiscal deve ter ao menos um item");
        }
        this.itens = new ArrayList<>(itens);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static NotaFiscal nova(UUID fornecedorId, UUID filialId, String numero, String serie,
                                  String chaveNfe, OffsetDateTime dataEmissao,
                                  BigDecimal valorTotal, String observacao,
                                  UUID createdByUsuarioId, UUID movimentacaoEntradaId,
                                  List<ItemNotaFiscal> itens, Instant agora) {
        return new NotaFiscal(NotaFiscalId.generate(), fornecedorId, filialId, numero, serie,
                chaveNfe, dataEmissao, agora, valorTotal, observacao, createdByUsuarioId,
                movimentacaoEntradaId, itens, agora, agora);
    }

    private static String exigirNaoVazio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new ValidationException(mensagem);
        }
        return valor.trim();
    }

    private static String validarChave(String chave) {
        if (chave == null || chave.isBlank()) return null;
        String c = chave.trim();
        if (c.length() != 44 || !c.chars().allMatch(Character::isDigit)) {
            throw new ValidationException("Chave NF-e deve ter 44 dígitos");
        }
        return c;
    }

    public NotaFiscalId id() { return id; }
    public UUID fornecedorId() { return fornecedorId; }
    public UUID filialId() { return filialId; }
    public String numero() { return numero; }
    public String serie() { return serie; }
    public Optional<String> chaveNfeOpt() { return Optional.ofNullable(chaveNfe); }
    public OffsetDateTime dataEmissao() { return dataEmissao; }
    public Instant dataLancamento() { return dataLancamento; }
    public BigDecimal valorTotal() { return valorTotal; }
    public Optional<String> observacaoOpt() { return Optional.ofNullable(observacao); }
    public UUID createdByUsuarioId() { return createdByUsuarioId; }
    public UUID movimentacaoEntradaId() { return movimentacaoEntradaId; }
    public List<ItemNotaFiscal> itens() { return Collections.unmodifiableList(itens); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
