package com.nonnas.inventory.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Movimentação imutável: nunca é alterada após criação. Auditoria é trilha,
 * não estado mutável. Toda alteração de estoque produz uma nova movimentação
 * (ENTRADA/SAÍDA) com seus itens. SaldoLote é projeção dessa trilha.
 */
public record Movimentacao(
        MovimentacaoId id,
        UUID filialId,
        UUID usuarioId,
        TipoMovimentacao tipo,
        Instant dataMovimentacao,
        String documentoOrigemTipo,
        UUID documentoOrigemId,
        String observacao,
        boolean gerouNegativo,
        List<ItemMovimentacao> itens,
        Instant createdAt
) {
    public Movimentacao {
        Objects.requireNonNull(id);
        Objects.requireNonNull(filialId);
        Objects.requireNonNull(usuarioId);
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(dataMovimentacao);
        Objects.requireNonNull(itens);
        Objects.requireNonNull(createdAt);
        if (itens.isEmpty()) {
            throw new ValidationException("Movimentação deve ter pelo menos um item");
        }
        // Defensive copy to enforce immutability of the list
        itens = List.copyOf(itens);
    }

    public static Movimentacao nova(UUID filialId, UUID usuarioId, TipoMovimentacao tipo,
                                    Instant dataMov, String docTipo, UUID docId, String observacao,
                                    boolean gerouNegativo, List<ItemMovimentacao> itens, Instant agora) {
        return new Movimentacao(MovimentacaoId.generate(), filialId, usuarioId, tipo, dataMov,
                docTipo, docId, observacao, gerouNegativo, itens, agora);
    }

    public Optional<String> documentoOrigemTipoOpt() { return Optional.ofNullable(documentoOrigemTipo); }
    public Optional<UUID> documentoOrigemIdOpt() { return Optional.ofNullable(documentoOrigemId); }
    public Optional<String> observacaoOpt() { return Optional.ofNullable(observacao); }
}
