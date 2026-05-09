package com.nonnas.recipes.interfaces.rest.dto;

import com.nonnas.recipes.domain.ProdutoVendavel;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class ProdutoVendavelDto {

    public record Request(
            @NotBlank String codigo,
            @NotBlank String nome,
            @NotBlank String categoria
    ) {}

    public record Response(
            UUID id,
            String codigo,
            String nome,
            String categoria,
            boolean ativo,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(ProdutoVendavel p) {
            return new Response(p.id().value(), p.codigo(), p.nome(), p.categoria(),
                    p.ativo(), p.createdAt(), p.updatedAt());
        }
    }

    private ProdutoVendavelDto() {}
}
