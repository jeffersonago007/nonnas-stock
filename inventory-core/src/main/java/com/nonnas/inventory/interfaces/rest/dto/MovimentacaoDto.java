package com.nonnas.inventory.interfaces.rest.dto;

import com.nonnas.inventory.domain.ItemMovimentacao;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class MovimentacaoDto {

    public record EntradaManualRequest(
            @NotNull UUID filialId,
            @NotNull UUID usuarioId,
            @NotNull UUID insumoId,
            UUID fornecedorId,
            UUID notaFiscalId,
            String numeroLote,
            LocalDate dataFabricacao,
            LocalDate dataValidade,
            @NotNull @Positive BigDecimal valorUnitario,
            @NotNull UUID unidadeLancamentoId,
            @NotNull @Positive BigDecimal quantidadeLancada,
            @NotNull @Positive BigDecimal quantidadeBase,
            @NotNull TipoMovimentacao tipo,
            String observacao
    ) {}

    public record SaidaManualRequest(
            @NotNull UUID filialId,
            @NotNull UUID usuarioId,
            @NotNull UUID insumoId,
            @NotNull UUID unidadeLancamentoId,
            @NotNull @Positive BigDecimal quantidadeBase,
            @NotNull TipoMovimentacao tipo,
            String observacao
    ) {}

    public record ItemResponse(
            UUID id,
            UUID insumoId,
            UUID loteId,
            UUID unidadeLancamentoId,
            BigDecimal quantidadeLancada,
            BigDecimal quantidadeBase,
            BigDecimal valorUnitario
    ) {
        public static ItemResponse from(ItemMovimentacao i) {
            return new ItemResponse(i.id(), i.insumoId(), i.loteId().value(),
                    i.unidadeLancamentoId(), i.quantidadeLancada(), i.quantidadeBase(),
                    i.valorUnitario());
        }
    }

    public record Response(
            UUID id,
            UUID filialId,
            UUID usuarioId,
            TipoMovimentacao tipo,
            Instant dataMovimentacao,
            String observacao,
            boolean gerouNegativo,
            List<ItemResponse> itens
    ) {
        public static Response from(Movimentacao m) {
            return new Response(m.id().value(), m.filialId(), m.usuarioId(),
                    m.tipo(), m.dataMovimentacao(), m.observacao(), m.gerouNegativo(),
                    m.itens().stream().map(ItemResponse::from).toList());
        }
    }

    public record SaldoResponse(UUID insumoId, UUID filialId, BigDecimal quantidadeBase) {}

    private MovimentacaoDto() {}
}
