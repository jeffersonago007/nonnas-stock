package com.nonnas.catalog.interfaces.rest.dto;

import com.nonnas.catalog.domain.ContatoFornecedor;
import com.nonnas.catalog.domain.Fornecedor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FornecedorDto {

    public record CreateRequest(
            @NotBlank String razaoSocial,
            @NotBlank String cnpj,
            @Valid List<ContatoRequest> contatos
    ) {}

    public record UpdateRequest(
            @NotBlank String razaoSocial,
            @Valid List<ContatoRequest> contatos
    ) {}

    /** Aceito tanto na criação quanto no update. Pelo menos um campo obrigatório (validado no domain). */
    public record ContatoRequest(String nome, String email, String telefone) {
        public ContatoFornecedor toDomain() {
            return ContatoFornecedor.novo(nome, email, telefone);
        }
    }

    public record ContatoResponse(UUID id, String nome, String email, String telefone) {
        public static ContatoResponse from(ContatoFornecedor c) {
            return new ContatoResponse(
                    c.id(), c.nomeOpt().orElse(null),
                    c.emailOpt().orElse(null), c.telefoneOpt().orElse(null));
        }
    }

    public record Response(
            UUID id,
            String razaoSocial,
            String cnpj,
            String cnpjFormatado,
            boolean ativo,
            List<ContatoResponse> contatos,
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
                    f.contatos().stream().map(ContatoResponse::from).toList(),
                    f.createdAt(),
                    f.updatedAt());
        }
    }

    private FornecedorDto() {}
}
