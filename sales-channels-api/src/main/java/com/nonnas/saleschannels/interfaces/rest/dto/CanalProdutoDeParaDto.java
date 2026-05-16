package com.nonnas.saleschannels.interfaces.rest.dto;

import com.nonnas.saleschannels.domain.CanalProdutoDePara;
import com.nonnas.saleschannels.domain.CanalTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class CanalProdutoDeParaDto {

    private CanalProdutoDeParaDto() {}

    public record CreateRequest(
            @NotNull CanalTipo canalTipo,
            @NotBlank String externalCode,
            UUID filialId,                   // null = global
            @NotNull UUID produtoVendavelId,
            String observacao
    ) {}

    public record UpdateRequest(
            @NotNull UUID produtoVendavelId,
            String observacao
    ) {}

    public record Response(
            UUID id,
            CanalTipo canalTipo,
            String externalCode,
            UUID filialId,
            UUID produtoVendavelId,
            String observacao,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(CanalProdutoDePara d) {
            return new Response(
                    d.id().value(), d.canalTipo(), d.externalCode(),
                    d.filialIdOpt().orElse(null), d.produtoVendavelId(),
                    d.observacaoOpt().orElse(null),
                    d.createdAt(), d.updatedAt());
        }
    }
}
