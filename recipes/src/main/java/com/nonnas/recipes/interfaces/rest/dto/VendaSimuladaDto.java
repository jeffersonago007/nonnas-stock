package com.nonnas.recipes.interfaces.rest.dto;

import com.nonnas.inventory.domain.ItemMovimentacao;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.recipes.application.venda.PreviewVendaSimuladaUseCase;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class VendaSimuladaDto {

    public record Request(
            @NotNull UUID produtoVendavelId,
            @NotNull UUID filialId,
            @NotNull UUID usuarioId,
            @NotNull @Positive BigDecimal quantidadeVendida,
            String observacao
    ) {}

    public record ItemResponse(
            UUID id,
            UUID insumoId,
            UUID loteId,
            UUID unidadeLancamentoId,
            BigDecimal quantidadeBase,
            BigDecimal valorUnitario
    ) {
        public static ItemResponse from(ItemMovimentacao i) {
            return new ItemResponse(i.id(), i.insumoId(), i.loteId().value(),
                    i.unidadeLancamentoId(), i.quantidadeBase(), i.valorUnitario());
        }
    }

    public record Response(
            UUID movimentacaoId,
            UUID filialId,
            TipoMovimentacao tipo,
            Instant dataMovimentacao,
            String documentoOrigemTipo,
            UUID documentoOrigemId,
            boolean gerouNegativo,
            List<ItemResponse> itens
    ) {
        public static Response from(Movimentacao m) {
            return new Response(
                    m.id().value(),
                    m.filialId(),
                    m.tipo(),
                    m.dataMovimentacao(),
                    m.documentoOrigemTipoOpt().orElse(null),
                    m.documentoOrigemIdOpt().orElse(null),
                    m.gerouNegativo(),
                    m.itens().stream().map(ItemResponse::from).toList()
            );
        }
    }

    public record PreviewRequest(
            @NotNull UUID produtoVendavelId,
            @NotNull UUID filialId,
            @NotNull @Positive BigDecimal quantidadeVendida
    ) {}

    public record LoteConsumidoResponse(
            UUID loteId,
            String numero,
            LocalDate validade,
            BigDecimal quantidade
    ) {
        public static LoteConsumidoResponse from(PreviewVendaSimuladaUseCase.LoteConsumido l) {
            return new LoteConsumidoResponse(l.loteId(), l.numero(), l.validade(), l.quantidade());
        }
    }

    public record ItemBaixaPreviewResponse(
            UUID insumoId,
            String insumoNome,
            BigDecimal quantidadeBase,
            String unidadeBase,
            boolean controlaValidade,
            List<LoteConsumidoResponse> lotes,
            BigDecimal saldoRestanteAposBaixa
    ) {
        public static ItemBaixaPreviewResponse from(PreviewVendaSimuladaUseCase.ItemBaixaPreview i) {
            return new ItemBaixaPreviewResponse(
                    i.insumoId(), i.insumoNome(), i.quantidadeBase(),
                    i.unidadeBase(), i.controlaValidade(),
                    i.lotes().stream().map(LoteConsumidoResponse::from).toList(),
                    i.saldoRestanteAposBaixa()
            );
        }
    }

    public record PreviewResponse(
            List<ItemBaixaPreviewResponse> itens,
            boolean gerouNegativo
    ) {
        public static PreviewResponse from(PreviewVendaSimuladaUseCase.Resposta r) {
            return new PreviewResponse(
                    r.itens().stream().map(ItemBaixaPreviewResponse::from).toList(),
                    r.gerouNegativo()
            );
        }
    }

    private VendaSimuladaDto() {}
}
