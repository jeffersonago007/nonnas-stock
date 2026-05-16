package com.nonnas.saleschannels.interfaces.rest.dto;

import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.CredencialCanal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs do CRUD de credenciais. {@code clientSecret} (em claro) só trafega
 * no Create/Rotate; nunca volta em Response — guardamos apenas a versão
 * cifrada no banco.
 */
public final class CredencialCanalDto {

    private CredencialCanalDto() {}

    public record CreateRequest(
            @NotNull CanalTipo canalTipo,
            @NotNull UUID filialId,
            @NotBlank String merchantExternoId,
            @NotBlank String clientId,
            @NotBlank String clientSecret,
            String baseUrl,
            String observacao
    ) {}

    public record UpdateRequest(
            String baseUrl,
            String observacao,
            String clientSecret  // null = não toca; preenchido = rotaciona
    ) {}

    public record Response(
            UUID id,
            CanalTipo canalTipo,
            UUID filialId,
            String merchantExternoId,
            String clientId,
            String baseUrl,
            boolean ativa,
            String observacao,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(CredencialCanal c) {
            return new Response(
                    c.id().value(), c.canalTipo(), c.filialId(),
                    c.merchantExternoId(), c.clientId(),
                    c.baseUrlOpt().orElse(null), c.ativa(),
                    c.observacaoOpt().orElse(null),
                    c.createdAt(), c.updatedAt());
        }
    }
}
