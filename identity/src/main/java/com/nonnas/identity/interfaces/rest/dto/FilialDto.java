package com.nonnas.identity.interfaces.rest.dto;

import com.nonnas.identity.domain.Filial;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class FilialDto {

    public record CreateRequest(
            @NotNull UUID empresaId,
            @NotBlank @Size(max = 255) String nome,
            @NotBlank String cnpj,
            String endereco
    ) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 255) String nome,
            String endereco
    ) {}

    public record Response(
            UUID id,
            UUID empresaId,
            String nome,
            String cnpj,
            String cnpjFormatado,
            String endereco,
            boolean ativa,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(Filial f) {
            return new Response(
                    f.id().value(),
                    f.empresaId().value(),
                    f.nome(),
                    f.cnpj().value(),
                    f.cnpj().formatted(),
                    f.endereco().orElse(null),
                    f.ativa(),
                    f.createdAt(),
                    f.updatedAt()
            );
        }
    }

    private FilialDto() {}
}
