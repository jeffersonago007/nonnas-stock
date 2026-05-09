package com.nonnas.operations.interfaces.rest.dto;

import com.nonnas.operations.domain.AjusteEstoque;
import com.nonnas.operations.domain.StatusAjuste;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class AjusteEstoqueDto {

    public record LancarRequest(
            @NotNull UUID filialId,
            @NotNull UUID insumoId,
            @NotNull UUID unidadeId,
            @NotNull BigDecimal quantidadeDiff,
            @NotBlank String motivo,
            @NotNull UUID solicitadoPor
    ) {}

    public record AprovarRequest(@NotNull UUID aprovadoPor) {}

    public record Response(
            UUID id,
            UUID filialId,
            UUID insumoId,
            UUID unidadeId,
            BigDecimal quantidadeDiff,
            String motivo,
            StatusAjuste status,
            boolean requerAprovacao,
            UUID solicitadoPor,
            UUID aprovadoPor,
            Instant dataSolicitacao,
            Instant dataAprovacao,
            UUID movId,
            UUID origemTransferenciaId,
            String rejeicaoMotivo
    ) {
        public static Response from(AjusteEstoque a) {
            return new Response(
                    a.id().value(),
                    a.filialId(), a.insumoId(), a.unidadeId(),
                    a.quantidadeDiff(), a.motivo(),
                    a.status(), a.requerAprovacao(),
                    a.solicitadoPor(),
                    a.aprovadoPorOpt().orElse(null),
                    a.dataSolicitacao(),
                    a.dataAprovacaoOpt().orElse(null),
                    a.movIdOpt().orElse(null),
                    a.origemTransferenciaIdOpt().orElse(null),
                    a.rejeicaoMotivoOpt().orElse(null)
            );
        }
    }

    private AjusteEstoqueDto() {}
}
