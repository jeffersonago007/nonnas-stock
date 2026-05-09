package com.nonnas.alerts.interfaces.rest.dto;

import com.nonnas.alerts.domain.AlertaDisparado;
import com.nonnas.alerts.domain.StatusAlerta;
import com.nonnas.alerts.domain.TipoAlerta;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class AlertaDisparadoDto {

    public record AcaoUsuarioRequest(@NotNull UUID usuarioId) {}

    public record Response(
            UUID id,
            UUID configId,
            TipoAlerta tipo,
            UUID insumoId,
            UUID filialId,
            UUID loteId,
            StatusAlerta status,
            BigDecimal saldoNoDisparo,
            String detalhe,
            Instant dataDisparo,
            Instant dataResolucao,
            Instant visualizadoEm,
            UUID visualizadoPor,
            UUID resolvidoPor
    ) {
        public static Response from(AlertaDisparado a) {
            return new Response(
                    a.id().value(), a.configId().value(),
                    a.tipo(), a.insumoId(), a.filialId(),
                    a.loteIdOpt().orElse(null),
                    a.status(),
                    a.saldoNoDisparoOpt().orElse(null),
                    a.detalheOpt().orElse(null),
                    a.dataDisparo(),
                    a.dataResolucaoOpt().orElse(null),
                    a.visualizadoEmOpt().orElse(null),
                    a.visualizadoPorOpt().orElse(null),
                    a.resolvidoPorOpt().orElse(null));
        }
    }

    private AlertaDisparadoDto() {}
}
