package com.nonnas.inventory.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record ItemMovimentacao(
        UUID id,
        UUID insumoId,
        LoteId loteId,
        UUID unidadeLancamentoId,
        BigDecimal quantidadeLancada,
        BigDecimal quantidadeBase,
        BigDecimal valorUnitario
) {
    public ItemMovimentacao {
        Objects.requireNonNull(id);
        Objects.requireNonNull(insumoId);
        Objects.requireNonNull(loteId);
        Objects.requireNonNull(unidadeLancamentoId);
        Objects.requireNonNull(quantidadeLancada);
        Objects.requireNonNull(quantidadeBase);
        Objects.requireNonNull(valorUnitario);
        if (quantidadeLancada.signum() <= 0) {
            throw new ValidationException("Quantidade lançada deve ser positiva");
        }
        if (quantidadeBase.signum() <= 0) {
            throw new ValidationException("Quantidade base deve ser positiva");
        }
        if (valorUnitario.signum() < 0) {
            throw new ValidationException("Valor unitário não pode ser negativo");
        }
    }

    public static ItemMovimentacao novo(UUID insumoId, LoteId loteId, UUID unidadeLancamentoId,
                                        BigDecimal qtdLancada, BigDecimal qtdBase, BigDecimal valorUnitario) {
        return new ItemMovimentacao(UUID.randomUUID(), insumoId, loteId, unidadeLancamentoId,
                qtdLancada, qtdBase, valorUnitario);
    }
}
