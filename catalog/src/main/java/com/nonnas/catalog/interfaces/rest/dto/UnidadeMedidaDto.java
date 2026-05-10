package com.nonnas.catalog.interfaces.rest.dto;

import com.nonnas.catalog.domain.UnidadeMedida;
import com.nonnas.catalog.domain.UnidadeMedidaTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class UnidadeMedidaDto {

    public record CreateRequest(
            @NotBlank String codigo,
            @NotBlank String nome,
            @NotNull UnidadeMedidaTipo tipo
    ) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 100) String nome
    ) {}

    public record Response(
            UUID id,
            String codigo,
            String nome,
            UnidadeMedidaTipo tipo,
            boolean ativa,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(UnidadeMedida u) {
            return new Response(u.id().value(), u.codigo(), u.nome(), u.tipo(),
                    u.ativa(), u.createdAt(), u.updatedAt());
        }
    }

    private UnidadeMedidaDto() {}
}
