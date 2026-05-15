package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.domain.CredencialCanal;
import com.nonnas.saleschannels.domain.CredencialCanalId;
import com.nonnas.saleschannels.domain.EventoCanal;
import com.nonnas.saleschannels.domain.EventoCanalId;
import com.nonnas.saleschannels.domain.ItemPedidoCanal;
import com.nonnas.saleschannels.domain.PedidoCanal;
import com.nonnas.saleschannels.domain.PedidoCanalId;

import java.util.List;

final class SalesChannelsMappers {

    private SalesChannelsMappers() {}

    // ────────────── CredencialCanal ──────────────

    static CredencialCanalEntity toEntity(CredencialCanal c) {
        CredencialCanalEntity e = new CredencialCanalEntity();
        e.setId(c.id().value());
        e.setCanalTipo(c.canalTipo());
        e.setFilialId(c.filialId());
        e.setMerchantExternoId(c.merchantExternoId());
        e.setClientId(c.clientId());
        e.setClientSecretCifrado(c.clientSecretCifrado());
        e.setBaseUrl(c.baseUrlOpt().orElse(null));
        e.setAtiva(c.ativa());
        e.setObservacao(c.observacaoOpt().orElse(null));
        e.setCreatedAt(c.createdAt());
        e.setUpdatedAt(c.updatedAt());
        return e;
    }

    static CredencialCanal toDomain(CredencialCanalEntity e) {
        return new CredencialCanal(
                CredencialCanalId.of(e.getId()), e.getCanalTipo(), e.getFilialId(),
                e.getMerchantExternoId(), e.getClientId(), e.getClientSecretCifrado(),
                e.getBaseUrl(), e.isAtiva(), e.getObservacao(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    // ────────────── PedidoCanal ──────────────

    static PedidoCanalEntity toEntity(PedidoCanal p, String payloadCanonicoJson, String payloadBrutoJson) {
        PedidoCanalEntity e = new PedidoCanalEntity();
        e.setId(p.id().value());
        e.setCanalTipo(p.canalTipo());
        e.setPedidoExternoId(p.pedidoExternoId());
        e.setDisplayId(p.displayIdOpt().orElse(null));
        e.setFilialId(p.filialId());
        e.setCredencialId(p.credencialId().value());
        e.setStatus(p.status());
        e.setValorTotal(p.valorTotal());
        e.setMoeda(p.moeda());
        e.setClienteNome(p.clienteNomeOpt().orElse(null));
        e.setClienteTelefone(p.clienteTelefoneOpt().orElse(null));
        e.setPayloadCanonicoJson(payloadCanonicoJson);
        e.setPayloadBrutoJson(payloadBrutoJson);
        e.setMovimentacaoId(p.movimentacaoIdOpt().orElse(null));
        e.setErroProcessamento(p.erroProcessamentoOpt().orElse(null));
        e.setRecebidoEm(p.recebidoEm());
        e.setProcessadoEm(p.processadoEmOpt().orElse(null));
        e.setConcluidoEm(p.concluidoEmOpt().orElse(null));
        e.setCanceladoEm(p.canceladoEmOpt().orElse(null));
        e.setItens(p.itens().stream().map(SalesChannelsMappers::toItemEntity).toList());
        return e;
    }

    /** Atualiza um entity já existente preservando payloads/itens originais. */
    static void aplicarMudancas(PedidoCanal p, PedidoCanalEntity e) {
        e.setStatus(p.status());
        e.setMovimentacaoId(p.movimentacaoIdOpt().orElse(null));
        e.setErroProcessamento(p.erroProcessamentoOpt().orElse(null));
        e.setProcessadoEm(p.processadoEmOpt().orElse(null));
        e.setConcluidoEm(p.concluidoEmOpt().orElse(null));
        e.setCanceladoEm(p.canceladoEmOpt().orElse(null));
        // Itens só atualizam produtoVendavelId — sequência+dados são imutáveis.
        for (ItemPedidoCanal item : p.itens()) {
            e.getItens().stream()
                    .filter(i -> i.getId().equals(item.id()))
                    .findFirst()
                    .ifPresent(i -> i.setProdutoVendavelId(item.produtoVendavelIdOpt().orElse(null)));
        }
    }

    static PedidoCanal toDomain(PedidoCanalEntity e) {
        List<ItemPedidoCanal> itens = e.getItens().stream()
                .map(SalesChannelsMappers::toItemDomain)
                .toList();
        return new PedidoCanal(
                PedidoCanalId.of(e.getId()), e.getCanalTipo(),
                e.getPedidoExternoId(), e.getDisplayId(),
                e.getFilialId(), CredencialCanalId.of(e.getCredencialId()),
                e.getStatus(), e.getValorTotal(), e.getMoeda(),
                e.getClienteNome(), e.getClienteTelefone(),
                itens,
                e.getMovimentacaoId(), e.getErroProcessamento(),
                e.getRecebidoEm(), e.getProcessadoEm(),
                e.getConcluidoEm(), e.getCanceladoEm());
    }

    private static ItemPedidoCanalEntity toItemEntity(ItemPedidoCanal i) {
        ItemPedidoCanalEntity e = new ItemPedidoCanalEntity();
        e.setId(i.id());
        e.setSequencia(i.sequencia());
        e.setExternalCode(i.externalCodeOpt().orElse(null));
        e.setNome(i.nome());
        e.setQuantidade(i.quantidade());
        e.setUnidade(i.unidade());
        e.setPrecoUnitario(i.precoUnitario());
        e.setPrecoTotal(i.precoTotal());
        e.setObservacao(i.observacaoOpt().orElse(null));
        e.setProdutoVendavelId(i.produtoVendavelIdOpt().orElse(null));
        return e;
    }

    private static ItemPedidoCanal toItemDomain(ItemPedidoCanalEntity e) {
        return new ItemPedidoCanal(
                e.getId(), e.getSequencia(), e.getExternalCode(), e.getNome(),
                e.getQuantidade(), e.getUnidade(),
                e.getPrecoUnitario(), e.getPrecoTotal(),
                e.getObservacao(), e.getProdutoVendavelId());
    }

    // ────────────── EventoCanal ──────────────

    static EventoCanalEntity toEntity(EventoCanal v) {
        EventoCanalEntity e = new EventoCanalEntity();
        e.setId(v.id().value());
        e.setCanalTipo(v.canalTipo());
        e.setEventIdExterno(v.eventIdExterno());
        e.setTipoEvento(v.tipoEvento());
        e.setPedidoExternoId(v.pedidoExternoIdOpt().orElse(null));
        e.setPedidoCanalId(v.pedidoCanalIdOpt().map(PedidoCanalId::value).orElse(null));
        e.setPayloadJson(v.payloadJson());
        e.setRecebidoEm(v.recebidoEm());
        e.setAcknowledgedEm(v.acknowledgedEmOpt().orElse(null));
        e.setProcessadoEm(v.processadoEmOpt().orElse(null));
        e.setErro(v.erroOpt().orElse(null));
        return e;
    }

    static EventoCanal toDomain(EventoCanalEntity e) {
        return new EventoCanal(
                EventoCanalId.of(e.getId()), e.getCanalTipo(),
                e.getEventIdExterno(), e.getTipoEvento(),
                e.getPedidoExternoId(),
                e.getPedidoCanalId() != null ? PedidoCanalId.of(e.getPedidoCanalId()) : null,
                e.getPayloadJson(), e.getRecebidoEm(),
                e.getAcknowledgedEm(), e.getProcessadoEm(), e.getErro());
    }
}
