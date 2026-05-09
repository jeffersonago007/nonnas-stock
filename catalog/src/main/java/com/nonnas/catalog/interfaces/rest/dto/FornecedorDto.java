package com.nonnas.catalog.interfaces.rest.dto;

import com.nonnas.catalog.domain.Fornecedor;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class FornecedorDto {

    public record CreateRequest(
            @NotBlank String razaoSocial,
            @NotBlank String cnpj
    ) {}

    public record UpdateRequest(
            @NotBlank String razaoSocial
    ) {}

    public record Response(
            UUID id,
            String razaoSocial,
            String cnpj,
            String cnpjFormatado,
            boolean ativo,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(Fornecedor f) {
            return new Response(
                    f.id().value(),
                    f.razaoSocial(),
                    f.cnpj().value(),
                    f.cnpj().formatted(),
                    f.ativo(),
                    f.createdAt(),
                    f.updatedAt());
        }
    }

    private FornecedorDto() {}
}
