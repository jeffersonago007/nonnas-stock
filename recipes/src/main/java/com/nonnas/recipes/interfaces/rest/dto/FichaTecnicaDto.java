package com.nonnas.recipes.interfaces.rest.dto;

import com.nonnas.recipes.domain.FichaTecnica;
import com.nonnas.recipes.domain.ItemFichaTecnica;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FichaTecnicaDto {

    public record Request(
            @NotEmpty @Valid List<ItemRequest> itens
    ) {}

    public record ItemRequest(
            @NotNull UUID insumoId,
            @NotNull UUID unidadeId,
            @NotNull @Positive BigDecimal quantidade
    ) {}

    public record ItemResponse(
            UUID id,
            UUID insumoId,
            UUID unidadeId,
            BigDecimal quantidade
    ) {
        public static ItemResponse from(ItemFichaTecnica i) {
            return new ItemResponse(i.id(), i.insumoId(), i.unidadeId(), i.quantidade());
        }
    }

    public record Response(
            UUID id,
            UUID produtoVendavelId,
            int versao,
            Instant vigenteDesde,
            Instant vigenteAte,
            boolean ativa,
            List<ItemResponse> itens
    ) {
        public static Response from(FichaTecnica f) {
            return new Response(
                    f.id().value(),
                    f.produtoVendavelId().value(),
                    f.versao(),
                    f.vigenteDesde(),
                    f.vigenteAteOpt().orElse(null),
                    f.ativa(),
                    f.itens().stream().map(ItemResponse::from).toList()
            );
        }
    }

    private FichaTecnicaDto() {}
}
