package com.nonnas.recipes.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Linha imutável da ficha técnica: insumo + unidade + quantidade.
 * Sem id tipado — segue padrão de {@code ItemMovimentacao} (T04).
 */
public record ItemFichaTecnica(
        UUID id,
        UUID insumoId,
        UUID unidadeId,
        BigDecimal quantidade
) {
    public ItemFichaTecnica {
        Objects.requireNonNull(id);
        Objects.requireNonNull(insumoId);
        Objects.requireNonNull(unidadeId);
        Objects.requireNonNull(quantidade);
        if (quantidade.signum() <= 0) {
            throw new ValidationException("Quantidade do item deve ser positiva");
        }
    }

    public static ItemFichaTecnica novo(UUID insumoId, UUID unidadeId, BigDecimal quantidade) {
        return new ItemFichaTecnica(UUID.randomUUID(), insumoId, unidadeId, quantidade);
    }
}
