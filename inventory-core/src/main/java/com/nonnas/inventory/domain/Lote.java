package com.nonnas.inventory.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Lote físico de um insumo recebido em uma entrada. Imutável após criação
 * — campos como validade não mudam (se mudou de validade, é outro lote).
 */
public record Lote(
        LoteId id,
        UUID insumoId,
        UUID fornecedorId,
        UUID notaFiscalId,
        String numeroLote,
        LocalDate dataFabricacao,
        LocalDate dataValidade,
        BigDecimal valorUnitario,
        Instant createdAt
) {
    public Lote {
        Objects.requireNonNull(id);
        Objects.requireNonNull(insumoId);
        Objects.requireNonNull(numeroLote);
        Objects.requireNonNull(valorUnitario);
        Objects.requireNonNull(createdAt);
        if (numeroLote.isBlank()) {
            throw new ValidationException("Número do lote é obrigatório");
        }
        if (valorUnitario.signum() < 0) {
            throw new ValidationException("Valor unitário não pode ser negativo");
        }
        if (dataValidade != null && dataFabricacao != null && dataValidade.isBefore(dataFabricacao)) {
            throw new ValidationException("Validade não pode ser anterior à fabricação");
        }
    }

    public static Lote novo(UUID insumoId, UUID fornecedorId, UUID notaFiscalId, String numeroLote,
                            LocalDate fabricacao, LocalDate validade, BigDecimal valorUnitario, Instant agora) {
        return new Lote(LoteId.generate(), insumoId, fornecedorId, notaFiscalId, numeroLote.trim(),
                fabricacao, validade, valorUnitario, agora);
    }

    public Optional<UUID> fornecedorIdOpt() { return Optional.ofNullable(fornecedorId); }
    public Optional<UUID> notaFiscalIdOpt() { return Optional.ofNullable(notaFiscalId); }
    public Optional<LocalDate> dataFabricacaoOpt() { return Optional.ofNullable(dataFabricacao); }
    public Optional<LocalDate> dataValidadeOpt() { return Optional.ofNullable(dataValidade); }
}
