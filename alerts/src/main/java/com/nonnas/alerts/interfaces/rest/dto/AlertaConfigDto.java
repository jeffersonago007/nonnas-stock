package com.nonnas.alerts.interfaces.rest.dto;

import com.nonnas.alerts.domain.AlertaConfig;
import com.nonnas.alerts.domain.TipoAlerta;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class AlertaConfigDto {

    public record CriarRequest(
            @NotNull TipoAlerta tipo,
            UUID insumoId,
            UUID filialId,
            BigDecimal threshold,
            int prioridade,
            String observacao
    ) {}

    public record AtualizarRequest(
            BigDecimal threshold,
            int prioridade,
            String observacao,
            boolean ativo
    ) {}

    public record Response(
            UUID id,
            TipoAlerta tipo,
            UUID insumoId,
            UUID filialId,
            BigDecimal threshold,
            boolean ativo,
            int prioridade,
            String observacao,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(AlertaConfig c) {
            return new Response(c.id().value(), c.tipo(),
                    c.insumoIdOpt().orElse(null), c.filialIdOpt().orElse(null),
                    c.thresholdOpt().orElse(null), c.ativo(), c.prioridade(),
                    c.observacaoOpt().orElse(null),
                    c.createdAt(), c.updatedAt());
        }
    }

    private AlertaConfigDto() {}
}
