package com.nonnas.identity.interfaces.rest.dto;

import com.nonnas.identity.application.password.SenhaValida;
import com.nonnas.identity.domain.Perfil;
import com.nonnas.identity.domain.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class UsuarioDto {

    public record CreateRequest(
            UUID filialId,
            @NotBlank @Size(max = 255) String nome,
            @NotBlank @Email String email,
            @NotNull @SenhaValida String senha,
            @NotNull Perfil perfil
    ) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 255) String nome
    ) {}

    public record Response(
            UUID id,
            UUID filialId,
            String nome,
            String email,
            Perfil perfil,
            boolean ativo,
            boolean travada,
            Instant bloqueadoAte,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(Usuario u) {
            return new Response(
                    u.id().value(),
                    u.filialId().map(f -> f.value()).orElse(null),
                    u.nome(),
                    u.email().value(),
                    u.perfil(),
                    u.ativo(),
                    u.travada(),
                    u.bloqueadoAte().orElse(null),
                    u.createdAt(),
                    u.updatedAt()
            );
        }
    }

    private UsuarioDto() {}
}
