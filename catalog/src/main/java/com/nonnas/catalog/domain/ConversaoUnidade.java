package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Define que multiplicar um valor expresso em {@code origemId} por
 * {@code fator} produz o valor equivalente em {@code destinoId}.
 *
 * <p>Quando {@code insumoId} é {@code null}, a conversão é <em>global</em>
 * — vale para qualquer insumo. Quando preenchido, é <em>específica</em>
 * desse insumo (ex.: caixa de mussarela de 5kg = 1 CX → 5 KG).
 *
 * <p>O {@link ConversorUnidadeService} deriva automaticamente a conversão
 * inversa (1/fator), de modo que basta cadastrar uma direção.
 */
public record ConversaoUnidade(
        UUID id,
        UnidadeMedidaId origemId,
        UnidadeMedidaId destinoId,
        BigDecimal fator,
        InsumoId insumoId,
        Instant createdAt
) {
    public ConversaoUnidade {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(origemId, "origemId");
        Objects.requireNonNull(destinoId, "destinoId");
        Objects.requireNonNull(fator, "fator");
        Objects.requireNonNull(createdAt, "createdAt");
        if (origemId.equals(destinoId)) {
            throw new ValidationException("Origem e destino devem ser unidades diferentes");
        }
        if (fator.signum() <= 0) {
            throw new ValidationException("Fator de conversão deve ser positivo");
        }
    }

    public static ConversaoUnidade global(UnidadeMedidaId origem, UnidadeMedidaId destino,
                                          BigDecimal fator, Instant agora) {
        return new ConversaoUnidade(UUID.randomUUID(), origem, destino, fator, null, agora);
    }

    public static ConversaoUnidade porInsumo(UnidadeMedidaId origem, UnidadeMedidaId destino,
                                             BigDecimal fator, InsumoId insumoId, Instant agora) {
        Objects.requireNonNull(insumoId, "insumoId");
        return new ConversaoUnidade(UUID.randomUUID(), origem, destino, fator, insumoId, agora);
    }

    public boolean isGlobal() {
        return insumoId == null;
    }

    public Optional<InsumoId> insumoIdOpt() {
        return Optional.ofNullable(insumoId);
    }
}
