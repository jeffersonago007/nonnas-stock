package com.nonnas.catalog.interfaces.rest.dto;

import com.nonnas.catalog.domain.CategoriaInsumo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class CategoriaInsumoDto {

    public record CreateRequest(
            @NotBlank @Size(max = 255) String nome,
            UUID categoriaPaiId
    ) {}

    public record Response(
            UUID id,
            UUID categoriaPaiId,
            String nome,
            boolean ativa,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(CategoriaInsumo c) {
            return new Response(
                    c.id().value(),
                    c.categoriaPaiId().map(p -> p.value()).orElse(null),
                    c.nome(),
                    c.ativa(),
                    c.createdAt(),
                    c.updatedAt());
        }
    }

    private CategoriaInsumoDto() {}
}
