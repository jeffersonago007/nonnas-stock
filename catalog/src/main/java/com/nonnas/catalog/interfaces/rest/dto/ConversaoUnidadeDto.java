package com.nonnas.catalog.interfaces.rest.dto;

import com.nonnas.catalog.domain.ConversaoUnidade;
import com.nonnas.catalog.domain.InsumoId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ConversaoUnidadeDto {

    public record CreateRequest(
            @NotNull UUID origemId,
            @NotNull UUID destinoId,
            @NotNull @Positive BigDecimal fator,
            UUID insumoId
    ) {}

    public record Response(
            UUID id,
            UUID origemId,
            UUID destinoId,
            BigDecimal fator,
            UUID insumoId,
            boolean global,
            Instant createdAt
    ) {
        public static Response from(ConversaoUnidade c) {
            return new Response(
                    c.id(),
                    c.origemId().value(),
                    c.destinoId().value(),
                    c.fator(),
                    c.insumoIdOpt().map(InsumoId::value).orElse(null),
                    c.isGlobal(),
                    c.createdAt());
        }
    }

    public record ConverterRequest(
            @NotNull @Positive BigDecimal valor,
            @NotNull UUID origemId,
            @NotNull UUID destinoId,
            UUID insumoId
    ) {}

    public record ConverterResponse(
            BigDecimal valorOrigem,
            UUID origemId,
            BigDecimal valorDestino,
            UUID destinoId,
            UUID insumoId
    ) {}

    private ConversaoUnidadeDto() {}
}
