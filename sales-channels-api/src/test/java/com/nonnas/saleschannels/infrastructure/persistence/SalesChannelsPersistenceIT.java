package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.application.ports.CredencialCanalRepository;
import com.nonnas.saleschannels.application.ports.EventoCanalRepository;
import com.nonnas.saleschannels.application.ports.PedidoCanalRepository;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.CredencialCanal;
import com.nonnas.saleschannels.domain.EventoCanal;
import com.nonnas.saleschannels.domain.ItemPedidoCanal;
import com.nonnas.saleschannels.domain.PedidoCanal;
import com.nonnas.saleschannels.domain.PedidoCanalId;
import com.nonnas.saleschannels.domain.StatusPedidoCanal;
import com.nonnas.saleschannels.domain.TipoEventoCanal;
import com.nonnas.saleschannels.testsupport.AbstractSalesChannelsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalesChannelsPersistenceIT extends AbstractSalesChannelsIntegrationTest {

    @Autowired private CredencialCanalRepository credenciais;
    @Autowired private PedidoCanalRepository pedidos;
    @Autowired private EventoCanalRepository eventos;

    private static final Instant T0 = Instant.parse("2026-05-15T18:00:00Z");

    // ────────────────── CredencialCanal ──────────────────

    @Test
    void salvaEBuscaCredencialAtiva() {
        UUID filial = UUID.randomUUID();
        CredencialCanal nova = CredencialCanal.nova(
                CanalTipo.IFOOD, filial, "merchant-mooca", "client-id",
                "AES256GCM:cifrado", "https://api.example.com", "POC", T0);
        CredencialCanal salva = credenciais.save(nova);

        Optional<CredencialCanal> achada = credenciais.findAtivaByCanalEFilial(CanalTipo.IFOOD, filial);
        assertThat(achada).isPresent();
        assertThat(achada.get().merchantExternoId()).isEqualTo("merchant-mooca");
        assertThat(achada.get().ativa()).isTrue();

        Optional<CredencialCanal> porMerchant = credenciais.findAtivaByMerchantExterno(CanalTipo.IFOOD, "merchant-mooca");
        assertThat(porMerchant).isPresent();
        assertThat(porMerchant.get().id()).isEqualTo(salva.id());
    }

    @Test
    void unicidadeDeCredencialAtivaPorCanalEFilial() {
        UUID filial = UUID.randomUUID();
        credenciais.save(CredencialCanal.nova(
                CanalTipo.IFOOD, filial, "m1", "c1", "AES256GCM:a", null, null, T0));

        CredencialCanal duplicada = CredencialCanal.nova(
                CanalTipo.IFOOD, filial, "m2", "c2", "AES256GCM:b", null, null, T0);
        assertThatThrownBy(() -> credenciais.save(duplicada))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void desativarLiberaEspacoParaNovaAtiva() {
        UUID filial = UUID.randomUUID();
        CredencialCanal velha = credenciais.save(CredencialCanal.nova(
                CanalTipo.IFOOD, filial, "m1", "c1", "AES256GCM:a", null, null, T0));
        velha.desativar(T0.plusSeconds(60));
        credenciais.save(velha);

        // Agora dá pra criar outra ativa pra mesma (canal, filial).
        CredencialCanal nova = credenciais.save(CredencialCanal.nova(
                CanalTipo.IFOOD, filial, "m2", "c2", "AES256GCM:b", null, null, T0.plusSeconds(120)));
        assertThat(nova.ativa()).isTrue();

        // findAtiva retorna só a nova (a velha está desativada).
        Optional<CredencialCanal> ativa = credenciais.findAtivaByCanalEFilial(CanalTipo.IFOOD, filial);
        assertThat(ativa).isPresent();
        assertThat(ativa.get().merchantExternoId()).isEqualTo("m2");
    }

    // ────────────────── PedidoCanal ──────────────────

    @Test
    void salvaPedidoComItensETransicionaEstado() {
        CredencialCanal cred = credenciais.save(CredencialCanal.nova(
                CanalTipo.IFOOD, UUID.randomUUID(), "merch-A", "c", "AES256GCM:x", null, null, T0));

        ItemPedidoCanal item = ItemPedidoCanal.novo(
                1, "PIZZA-MARG", "Pizza Margherita",
                new BigDecimal("1"), "UN",
                new BigDecimal("49.90"), new BigDecimal("49.90"), null);
        PedidoCanal pedido = PedidoCanal.recebido(
                CanalTipo.IFOOD, "ext-order-1", "#001",
                cred.filialId(), cred.id(),
                new BigDecimal("49.90"), "BRL",
                "Maria", "+5511999990000",
                List.of(item), T0);

        PedidoCanal salvo = pedidos.salvarNovo(pedido, "{\"id\":\"ext-order-1\"}", "{\"raw\":true}");
        assertThat(salvo.id()).isNotNull();
        assertThat(salvo.itens()).hasSize(1);
        assertThat(salvo.status()).isEqualTo(StatusPedidoCanal.RECEBIDO);

        // Transição: RECEBIDO → EM_PROCESSAMENTO → CONFIRMADO_ESTOQUE.
        salvo.iniciarProcessamento(T0.plusSeconds(5));
        salvo.confirmarEstoque(UUID.randomUUID(), T0.plusSeconds(10));
        PedidoCanal apos = pedidos.atualizar(salvo);
        assertThat(apos.status()).isEqualTo(StatusPedidoCanal.CONFIRMADO_ESTOQUE);
        assertThat(apos.movimentacaoIdOpt()).isPresent();

        Optional<PedidoCanal> recarregado = pedidos.findById(apos.id());
        assertThat(recarregado).isPresent();
        assertThat(recarregado.get().status()).isEqualTo(StatusPedidoCanal.CONFIRMADO_ESTOQUE);
        assertThat(recarregado.get().itens()).hasSize(1);
    }

    @Test
    void unicidadeDePedidoPorCanalEPedidoExterno() {
        CredencialCanal cred = credenciais.save(CredencialCanal.nova(
                CanalTipo.IFOOD, UUID.randomUUID(), "merch-B", "c", "AES256GCM:x", null, null, T0));
        ItemPedidoCanal item = ItemPedidoCanal.novo(
                1, "X", "Item", new BigDecimal("1"), "UN",
                BigDecimal.ONE, BigDecimal.ONE, null);
        PedidoCanal p1 = PedidoCanal.recebido(
                CanalTipo.IFOOD, "ext-dup", null,
                cred.filialId(), cred.id(),
                BigDecimal.ONE, "BRL", null, null, List.of(item), T0);
        pedidos.salvarNovo(p1, "{}", null);

        PedidoCanal p2 = PedidoCanal.recebido(
                CanalTipo.IFOOD, "ext-dup", null,
                cred.filialId(), cred.id(),
                BigDecimal.ONE, "BRL", null, null,
                List.of(ItemPedidoCanal.novo(1, "Y", "Outro", BigDecimal.ONE, "UN",
                        BigDecimal.ONE, BigDecimal.ONE, null)), T0);
        assertThatThrownBy(() -> pedidos.salvarNovo(p2, "{}", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void listarPorFilialEStatusOrdenaPorRecebidoDesc() {
        CredencialCanal cred = credenciais.save(CredencialCanal.nova(
                CanalTipo.IFOOD, UUID.randomUUID(), "merch-C", "c", "AES256GCM:x", null, null, T0));

        for (int i = 0; i < 3; i++) {
            ItemPedidoCanal item = ItemPedidoCanal.novo(
                    1, "X" + i, "Item " + i, BigDecimal.ONE, "UN",
                    BigDecimal.ONE, BigDecimal.ONE, null);
            PedidoCanal p = PedidoCanal.recebido(
                    CanalTipo.IFOOD, "order-" + i, null,
                    cred.filialId(), cred.id(),
                    BigDecimal.ONE, "BRL", null, null,
                    List.of(item), T0.plusSeconds(i * 10L));
            pedidos.salvarNovo(p, "{}", null);
        }

        List<PedidoCanal> recebidos = pedidos.listarPorFilialEStatus(cred.filialId(), StatusPedidoCanal.RECEBIDO);
        assertThat(recebidos).hasSize(3);
        assertThat(recebidos.get(0).recebidoEm()).isAfter(recebidos.get(1).recebidoEm());
    }

    // ────────────────── EventoCanal ──────────────────

    @Test
    void salvaSeNovoIdempotenteRetornaEmptyEmEventDuplicado() {
        EventoCanal e1 = EventoCanal.recebido(
                CanalTipo.IFOOD, "evt-001", TipoEventoCanal.PEDIDO_CRIADO,
                "ord-1", "{\"foo\":\"bar\"}", T0);
        Optional<EventoCanal> primeiro = eventos.salvarSeNovo(e1);
        assertThat(primeiro).isPresent();

        // Reentrega — mesmo event_id, deve retornar empty.
        EventoCanal e2 = EventoCanal.recebido(
                CanalTipo.IFOOD, "evt-001", TipoEventoCanal.PEDIDO_CRIADO,
                "ord-1", "{\"foo\":\"bar\"}", T0.plusSeconds(30));
        Optional<EventoCanal> segundo = eventos.salvarSeNovo(e2);
        assertThat(segundo).isEmpty();

        assertThat(eventos.findByCanalEEventIdExterno(CanalTipo.IFOOD, "evt-001")).isPresent();
    }

    @Test
    void atualizarEventoMarcaAckEProcessadoEVinculaPedido() {
        // FK fk_eventos_canais_pedido exige um pedido real.
        CredencialCanal cred = credenciais.save(CredencialCanal.nova(
                CanalTipo.IFOOD, UUID.randomUUID(), "merch-evt", "c", "AES256GCM:x", null, null, T0));
        ItemPedidoCanal item = ItemPedidoCanal.novo(
                1, "X", "Item", BigDecimal.ONE, "UN",
                BigDecimal.ONE, BigDecimal.ONE, null);
        PedidoCanal pedido = pedidos.salvarNovo(
                PedidoCanal.recebido(
                        CanalTipo.IFOOD, "ord-100-real", null,
                        cred.filialId(), cred.id(),
                        BigDecimal.ONE, "BRL", null, null, List.of(item), T0),
                "{}", null);

        EventoCanal e = eventos.salvarSeNovo(EventoCanal.recebido(
                CanalTipo.IFOOD, "evt-100", TipoEventoCanal.PEDIDO_CONFIRMADO,
                "ord-100-real", "{}", T0)).orElseThrow();

        e.marcarAcknowledged(T0.plusSeconds(1));
        e.vincularPedido(pedido.id());
        e.marcarProcessado(T0.plusSeconds(2));
        EventoCanal atualizado = eventos.atualizar(e);
        assertThat(atualizado.acknowledgedEmOpt()).isPresent();
        assertThat(atualizado.processadoEmOpt()).isPresent();
        assertThat(atualizado.pedidoCanalIdOpt()).contains(pedido.id());
    }

    @Test
    void listarNaoProcessadosRetornaApenasSemProcessadoEm() {
        eventos.salvarSeNovo(EventoCanal.recebido(
                CanalTipo.IFOOD, "evt-a", TipoEventoCanal.PEDIDO_CRIADO, "o1", "{}", T0));
        EventoCanal processado = eventos.salvarSeNovo(EventoCanal.recebido(
                CanalTipo.IFOOD, "evt-b", TipoEventoCanal.PEDIDO_CRIADO, "o2", "{}", T0)).orElseThrow();
        processado.marcarProcessado(T0.plusSeconds(1));
        eventos.atualizar(processado);

        List<EventoCanal> pendentes = eventos.listarNaoProcessados(10);
        assertThat(pendentes).extracting(EventoCanal::eventIdExterno).contains("evt-a").doesNotContain("evt-b");
    }
}
