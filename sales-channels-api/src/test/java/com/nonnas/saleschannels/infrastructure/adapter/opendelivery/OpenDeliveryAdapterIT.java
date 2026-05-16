package com.nonnas.saleschannels.infrastructure.adapter.opendelivery;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nonnas.saleschannels.application.opendelivery.EventoBruto;
import com.nonnas.saleschannels.application.opendelivery.PedidoVendaCanonico;
import com.nonnas.saleschannels.application.ports.CredencialCanalRepository;
import com.nonnas.saleschannels.application.ports.EventoCanalRepository;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.CredencialCanal;
import com.nonnas.saleschannels.domain.TipoEventoCanal;
import com.nonnas.saleschannels.infrastructure.schedule.CanalPollingScheduler;
import com.nonnas.saleschannels.testsupport.AbstractSalesChannelsIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * IT do {@link OpenDeliveryAdapter} contra WireMock — exercita o ciclo
 * completo: credencial cadastrada → polling de eventos → fetch de pedido
 * → mapping pra canônico Open Delivery → state actions (confirm/dispatch/
 * conclude/cancel). Simula o que aconteceria contra o Prism mock real.
 */
class OpenDeliveryAdapterIT extends AbstractSalesChannelsIntegrationTest {

    @Autowired private OpenDeliveryAdapter adapter;
    @Autowired private CanalPollingScheduler scheduler;
    @Autowired private CredencialCanalRepository credenciais;
    @Autowired private EventoCanalRepository eventos;

    private WireMockServer wm;

    @BeforeEach
    void startMock() {
        wm = new WireMockServer(wireMockConfig().dynamicPort());
        wm.start();
        // Isola entre testes: desativa credenciais ativas de testes anteriores
        // que apontam para WireMocks já parados. Cada teste cadastra a sua.
        Instant agora = Instant.parse("2026-05-15T18:00:00Z");
        credenciais.listarPorCanal(CanalTipo.OPEN_DELIVERY_GENERICO).forEach(c -> {
            if (c.ativa()) {
                c.desativar(agora);
                credenciais.save(c);
            }
        });
        credenciais.save(CredencialCanal.nova(
                CanalTipo.OPEN_DELIVERY_GENERICO, UUID.randomUUID(),
                "merchant-test", "client-test", "AES256GCM:secret",
                "http://localhost:" + wm.port(), "wiremock test",
                agora));
    }

    @AfterEach
    void stopMock() {
        if (wm != null) wm.stop();
    }

    @Test
    void consumirEventosTraduzPLCParaPedidoCriado() {
        wm.stubFor(get(urlPathMatching("/events:polling"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            [
                              {"id":"evt-001","code":"PLC","orderId":"order-1","merchantId":"merchant-test","createdAt":"2026-05-15T18:00:00Z"},
                              {"id":"evt-002","code":"CFM","orderId":"order-1","merchantId":"merchant-test","createdAt":"2026-05-15T18:01:00Z"}
                            ]
                            """)));

        List<EventoBruto> brutos = adapter.consumirEventos(50);

        assertThat(brutos).hasSize(2);
        assertThat(brutos.get(0).eventIdExterno()).isEqualTo("evt-001");
        assertThat(brutos.get(0).tipoEvento()).isEqualTo(TipoEventoCanal.PEDIDO_CRIADO);
        assertThat(brutos.get(0).pedidoExternoId()).isEqualTo("order-1");
        assertThat(brutos.get(0).payloadJson()).contains("PLC");

        assertThat(brutos.get(1).tipoEvento()).isEqualTo(TipoEventoCanal.PEDIDO_CONFIRMADO);
    }

    @Test
    void buscarPedidoMapeiaResponseCompletoParaCanonico() {
        wm.stubFor(get(urlPathEqualTo("/orders/order-xyz"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "id": "order-xyz",
                              "displayId": "#A1B2",
                              "type": "DELIVERY",
                              "salesChannel": "Open Delivery",
                              "createdAt": "2026-05-15T18:00:00Z",
                              "merchant": {"id":"merchant-test","name":"Nonnas Mooca"},
                              "customer": {"id":"cust-1","name":"Maria","phone":"+5511999990000"},
                              "items": [
                                {
                                  "id":"item-1","index":1,"externalCode":"PIZZA-MARG","name":"Pizza Margherita",
                                  "quantity":1,"unit":"UN",
                                  "unitPrice":{"value":49.90,"currency":"BRL"},
                                  "totalPrice":{"value":49.90,"currency":"BRL"},
                                  "observations":"sem cebola"
                                }
                              ],
                              "total": {
                                "itemsPrice":49.90,"otherFees":0,"discount":0,
                                "orderAmount":49.90,"currency":"BRL"
                              },
                              "extraInfo":"observação livre"
                            }
                            """)));

        PedidoVendaCanonico pedido = adapter.buscarPedido("order-xyz");

        assertThat(pedido.id()).isEqualTo("order-xyz");
        assertThat(pedido.displayId()).isEqualTo("#A1B2");
        assertThat(pedido.merchant().name()).isEqualTo("Nonnas Mooca");
        assertThat(pedido.customer().name()).isEqualTo("Maria");
        assertThat(pedido.items()).hasSize(1);
        assertThat(pedido.items().get(0).externalCode()).isEqualTo("PIZZA-MARG");
        assertThat(pedido.items().get(0).quantity()).isEqualByComparingTo("1");
        assertThat(pedido.total().orderAmount()).isEqualByComparingTo("49.90");
    }

    @Test
    void acknowledgeEnviaListaDeIds() {
        wm.stubFor(post(urlEqualTo("/events/acknowledgment"))
                .willReturn(aResponse().withStatus(202)));

        adapter.acknowledgeEvento("evt-001");

        wm.verify(postRequestedFor(urlEqualTo("/events/acknowledgment")));
    }

    @Test
    void stateActionsChamamEndpointsCorretos() {
        wm.stubFor(post(urlPathMatching("/orders/.*/confirm")).willReturn(aResponse().withStatus(202)));
        wm.stubFor(post(urlPathMatching("/orders/.*/dispatch")).willReturn(aResponse().withStatus(202)));
        wm.stubFor(post(urlPathMatching("/orders/.*/conclude")).willReturn(aResponse().withStatus(202)));
        wm.stubFor(post(urlPathMatching("/orders/.*/cancellation")).willReturn(aResponse().withStatus(202)));

        adapter.confirmarPedido("o1");
        adapter.despacharPedido("o1");
        adapter.concluirPedido("o1");
        adapter.cancelarPedido("o1", "OUT_OF_STOCK");

        wm.verify(postRequestedFor(urlPathEqualTo("/orders/o1/confirm")));
        wm.verify(postRequestedFor(urlPathEqualTo("/orders/o1/dispatch")));
        wm.verify(postRequestedFor(urlPathEqualTo("/orders/o1/conclude")));
        wm.verify(postRequestedFor(urlPathEqualTo("/orders/o1/cancellation")));
    }

    @Test
    void schedulerPersisteEventosEEhIdempotente() {
        wm.stubFor(get(urlPathMatching("/events:polling"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            [
                              {"id":"sched-evt-1","code":"PLC","orderId":"sched-o1","createdAt":"2026-05-15T18:00:00Z"},
                              {"id":"sched-evt-2","code":"PLC","orderId":"sched-o2","createdAt":"2026-05-15T18:01:00Z"}
                            ]
                            """)));

        int primeira = scheduler.pollCanal(CanalTipo.OPEN_DELIVERY_GENERICO);
        assertThat(primeira).isEqualTo(2);
        assertThat(eventos.findByCanalEEventIdExterno(CanalTipo.OPEN_DELIVERY_GENERICO, "sched-evt-1")).isPresent();

        // Segunda passada — mesmos event_ids, idempotência kicks in.
        int segunda = scheduler.pollCanal(CanalTipo.OPEN_DELIVERY_GENERICO);
        assertThat(segunda).isZero();
    }

    @Test
    void schedulerEngoleErroDoCanal() {
        wm.stubFor(get(urlPathMatching("/events:polling"))
                .willReturn(aResponse().withStatus(503)));

        int novos = scheduler.pollCanal(CanalTipo.OPEN_DELIVERY_GENERICO);

        // Não propaga — log e segue. Próximo ciclo tenta de novo.
        assertThat(novos).isZero();
    }
}
