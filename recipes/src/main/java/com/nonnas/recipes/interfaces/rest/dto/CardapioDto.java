package com.nonnas.recipes.interfaces.rest.dto;

import com.nonnas.recipes.application.cardapio.ListarCardapioUseCase;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class CardapioDto {

    public record Resposta(List<Item> itens) {
        public static Resposta from(ListarCardapioUseCase.Resposta r) {
            return new Resposta(r.itens().stream()
                    .map(i -> new Item(
                            i.origem().name(), i.id(), i.codigo(), i.nome(),
                            i.categoria(), i.unidadeBaseCodigo(), i.saldoNaFilial(),
                            i.vendasUltimos30Dias()))
                    .toList());
        }
    }

    public record Item(
            String origem,                // PRODUTO_FABRICADO | PRODUTO_REVENDA | INSUMO_ORFAO
            UUID id,                       // produtoVendavelId ou insumoId conforme origem
            String codigo,
            String nome,
            String categoria,
            String unidadeBaseCodigo,
            BigDecimal saldoNaFilial,
            int vendasUltimos30Dias        // 0 quando sem vendas no período / insumo órfão
    ) {}

    public record VenderInsumoRequest(
            @NotNull UUID insumoId,
            @NotNull UUID filialId,
            @NotNull UUID usuarioId,
            @NotNull @Positive BigDecimal quantidadeVendida,
            String observacao
    ) {}

    public record VendaInsumoResposta(
            UUID produtoVendavelCriadoId,
            UUID movimentacaoId,
            boolean gerouNegativo
    ) {}

    private CardapioDto() {}
}
