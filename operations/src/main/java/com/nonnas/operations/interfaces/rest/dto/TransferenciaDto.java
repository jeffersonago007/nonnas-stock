package com.nonnas.operations.interfaces.rest.dto;

import com.nonnas.operations.domain.ItemTransferencia;
import com.nonnas.operations.domain.StatusTransferencia;
import com.nonnas.operations.domain.Transferencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TransferenciaDto {

    public record SolicitarRequest(
            @NotNull UUID filialOrigemId,
            @NotNull UUID filialDestinoId,
            @NotNull UUID solicitadoPor,
            String observacao,
            @NotEmpty @Valid List<ItemRequest> itens
    ) {}

    public record ItemRequest(
            @NotNull UUID insumoId,
            @NotNull UUID unidadeId,
            @NotNull @Positive BigDecimal quantidade
    ) {}

    public record AcaoSimplesRequest(@NotNull UUID usuarioId) {}

    public record CancelarRequest(@NotNull String motivo) {}

    public record RecebimentoRequest(
            @NotNull UUID recebidoPor,
            @NotNull @Valid List<ItemRecebido> itens
    ) {}

    public record ItemRecebido(
            @NotNull UUID itemId,
            @NotNull BigDecimal quantidadeRecebida
    ) {}

    public record ItemResponse(
            UUID id,
            UUID insumoId,
            UUID unidadeId,
            BigDecimal quantidadeSolicitada,
            BigDecimal quantidadeRecebida
    ) {
        public static ItemResponse from(ItemTransferencia i) {
            return new ItemResponse(i.id(), i.insumoId(), i.unidadeId(),
                    i.quantidadeSolicitada(),
                    i.quantidadeRecebidaOpt().orElse(null));
        }
    }

    public record Response(
            UUID id,
            UUID filialOrigemId,
            UUID filialDestinoId,
            StatusTransferencia status,
            UUID solicitadoPor,
            UUID aprovadoPor,
            UUID enviadoPor,
            UUID recebidoPor,
            Instant dataSolicitacao,
            Instant dataAprovacao,
            Instant dataEnvio,
            Instant dataRecebimento,
            String observacao,
            UUID movSaidaId,
            UUID movEntradaId,
            String motivoCancelamento,
            List<ItemResponse> itens
    ) {
        public static Response from(Transferencia t) {
            return new Response(
                    t.id().value(),
                    t.filialOrigemId(), t.filialDestinoId(),
                    t.status(),
                    t.solicitadoPor(),
                    t.aprovadoPorOpt().orElse(null),
                    t.enviadoPorOpt().orElse(null),
                    t.recebidoPorOpt().orElse(null),
                    t.dataSolicitacao(),
                    t.dataAprovacaoOpt().orElse(null),
                    t.dataEnvioOpt().orElse(null),
                    t.dataRecebimentoOpt().orElse(null),
                    t.observacaoOpt().orElse(null),
                    t.movSaidaIdOpt().orElse(null),
                    t.movEntradaIdOpt().orElse(null),
                    t.motivoCancelamentoOpt().orElse(null),
                    t.itens().stream().map(ItemResponse::from).toList()
            );
        }
    }

    public record EmTransitoResponse(UUID insumoId, BigDecimal quantidadeEmTransito) {}

    private TransferenciaDto() {}
}
