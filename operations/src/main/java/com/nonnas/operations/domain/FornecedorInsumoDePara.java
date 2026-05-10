package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Mapeamento aprendido (fornecedor + código no XML) → insumo do catálogo.
 * Cada confirmação de lançamento de nota persiste/atualiza esta entrada.
 * Notas seguintes do mesmo fornecedor consultam aqui antes de sugerir
 * "criar novo insumo" no preview, eliminando retrabalho.
 */
public final class FornecedorInsumoDePara {

    private final UUID id;
    private final UUID fornecedorId;
    private final String codigoFornecedor;
    private final UUID insumoId;
    private final Instant createdAt;
    private Instant lastUsedAt;

    public FornecedorInsumoDePara(UUID id, UUID fornecedorId, String codigoFornecedor,
                                  UUID insumoId, Instant createdAt, Instant lastUsedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.fornecedorId = Objects.requireNonNull(fornecedorId, "fornecedorId");
        this.codigoFornecedor = exigirCodigo(codigoFornecedor);
        this.insumoId = Objects.requireNonNull(insumoId, "insumoId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.lastUsedAt = Objects.requireNonNull(lastUsedAt, "lastUsedAt");
    }

    public static FornecedorInsumoDePara novo(UUID fornecedorId, String codigoFornecedor,
                                              UUID insumoId, Instant agora) {
        return new FornecedorInsumoDePara(UUID.randomUUID(), fornecedorId, codigoFornecedor,
                insumoId, agora, agora);
    }

    public void marcarComoUsado(Instant agora) {
        this.lastUsedAt = Objects.requireNonNull(agora);
    }

    private static String exigirCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ValidationException("Código do fornecedor é obrigatório no de-para");
        }
        return codigo.trim();
    }

    public UUID id() { return id; }
    public UUID fornecedorId() { return fornecedorId; }
    public String codigoFornecedor() { return codigoFornecedor; }
    public UUID insumoId() { return insumoId; }
    public Instant createdAt() { return createdAt; }
    public Instant lastUsedAt() { return lastUsedAt; }
}
