package com.nonnas.identity.interfaces.rest.dto;

import com.nonnas.identity.domain.Empresa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class EmpresaDto {

    public record CreateRequest(
            @NotBlank @Size(max = 255) String razaoSocial,
            @NotBlank String cnpj
    ) {}

    public record Response(
            UUID id,
            String razaoSocial,
            String cnpj,
            String cnpjFormatado,
            boolean ativa,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(Empresa e) {
            return new Response(
                    e.id().value(),
                    e.razaoSocial().value(),
                    e.cnpj().value(),
                    e.cnpj().formatted(),
                    e.ativa(),
                    e.createdAt(),
                    e.updatedAt()
            );
        }
    }

    private EmpresaDto() {}
}
