package com.nonnas.saleschannels.interfaces.rest.dto;

import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.ItemPedidoCanal;
import com.nonnas.saleschannels.domain.PedidoCanal;
import com.nonnas.saleschannels.domain.StatusPedidoCanal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PedidoCanalDto {

    private PedidoCanalDto() {}

    public record Response(
            UUID id,
            CanalTipo canalTipo,
            String pedidoExternoId,
            String displayId,
            UUID filialId,
            UUID credencialId,
            StatusPedidoCanal status,
            BigDecimal valorTotal,
            String moeda,
            String clienteNome,
            String clienteTelefone,
            List<ItemResponse> itens,
            UUID movimentacaoId,
            String erroProcessamento,
            Instant recebidoEm,
            Instant processadoEm,
            Instant concluidoEm,
            Instant canceladoEm
    ) {
        public static Response from(PedidoCanal p) {
            return new Response(
                    p.id().value(), p.canalTipo(),
                    p.pedidoExternoId(), p.displayIdOpt().orElse(null),
                    p.filialId(), p.credencialId().value(),
                    p.status(), p.valorTotal(), p.moeda(),
                    p.clienteNomeOpt().orElse(null), p.clienteTelefoneOpt().orElse(null),
                    p.itens().stream().map(ItemResponse::from).toList(),
                    p.movimentacaoIdOpt().orElse(null),
                    p.erroProcessamentoOpt().orElse(null),
                    p.recebidoEm(),
                    p.processadoEmOpt().orElse(null),
                    p.concluidoEmOpt().orElse(null),
                    p.canceladoEmOpt().orElse(null));
        }
    }

    public record ItemResponse(
            int sequencia,
            String externalCode,
            String nome,
            BigDecimal quantidade,
            String unidade,
            BigDecimal precoUnitario,
            BigDecimal precoTotal,
            String observacao,
            UUID produtoVendavelId
    ) {
        public static ItemResponse from(ItemPedidoCanal i) {
            return new ItemResponse(
                    i.sequencia(),
                    i.externalCodeOpt().orElse(null),
                    i.nome(),
                    i.quantidade(),
                    i.unidade(),
                    i.precoUnitario(),
                    i.precoTotal(),
                    i.observacaoOpt().orElse(null),
                    i.produtoVendavelIdOpt().orElse(null));
        }
    }
}
