package com.nonnas.catalog.interfaces.rest.dto;

import com.nonnas.catalog.domain.Insumo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class InsumoDto {

    public record CreateRequest(
            @NotBlank @Size(max = 50) String codigo,
            @NotBlank @Size(max = 255) String nome,
            @NotNull UUID categoriaId,
            @NotNull UUID unidadeBaseId,
            Boolean controlaLote,
            Boolean controlaValidade
    ) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 255) String nome,
            UUID categoriaId,
            Boolean controlaLote,
            Boolean controlaValidade
    ) {}

    public record Response(
            UUID id,
            String codigo,
            String nome,
            UUID categoriaId,
            UUID unidadeBaseId,
            boolean controlaLote,
            boolean controlaValidade,
            boolean ativo,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(Insumo i) {
            return new Response(i.id().value(), i.codigo(), i.nome(),
                    i.categoriaId().value(), i.unidadeBaseId().value(),
                    i.controlaLote(), i.controlaValidade(), i.ativo(),
                    i.createdAt(), i.updatedAt());
        }
    }

    private InsumoDto() {}
}
