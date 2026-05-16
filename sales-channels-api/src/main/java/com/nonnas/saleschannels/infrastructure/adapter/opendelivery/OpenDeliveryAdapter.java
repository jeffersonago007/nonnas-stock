package com.nonnas.saleschannels.infrastructure.adapter.opendelivery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonnas.saleschannels.application.opendelivery.EventoBruto;
import com.nonnas.saleschannels.application.opendelivery.PedidoVendaCanonico;
import com.nonnas.saleschannels.application.ports.CanalAdapter;
import com.nonnas.saleschannels.application.ports.CredencialCanalRepository;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.CredencialCanal;
import com.nonnas.sharedkernel.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapter genérico para qualquer canal que falha o padrão Open Delivery
 * (v1.0.1). Usa as URLs configuradas na credencial — não autentica (mock
 * Prism + spec pública não exigem auth).
 *
 * <p>Para iFood real, virá um {@code IfoodAdapter} separado que herda este
 * mapper mas adiciona OAuth2 client-credentials. Fica para T-CANAL-03
 * follow-up quando a credencial iFood for emitida.
 *
 * <p>Roteamento por canal: o adapter aceita {@code CanalTipo.OPEN_DELIVERY_GENERICO}.
 * Outros canais (IFOOD, KEETA, NOVENTANOVE_FOOD) precisariam de adapter
 * específico — mas, como todos implementam Open Delivery, podemos
 * configurá-los para usar este adapter (sem auth) em ambiente de mock,
 * trocando só a baseUrl.
 *
 * @see <a href="https://abrasel-nacional.github.io/docs/versions/1.0.1/">Open Delivery v1.0.1</a>
 */
@Component
public class OpenDeliveryAdapter implements CanalAdapter {

    private static final Logger log = LoggerFactory.getLogger(OpenDeliveryAdapter.class);

    private final RestClient http;
    private final ObjectMapper jackson;
    private final CredencialCanalRepository credenciais;

    public OpenDeliveryAdapter(@Qualifier("salesChannelsRestClient") RestClient http,
                               ObjectMapper jackson,
                               CredencialCanalRepository credenciais) {
        this.http = http;
        this.jackson = jackson;
        this.credenciais = credenciais;
    }

    @Override
    public CanalTipo canal() {
        return CanalTipo.OPEN_DELIVERY_GENERICO;
    }

    @Override
    public PedidoVendaCanonico buscarPedido(String pedidoExternoId) {
        String baseUrl = baseUrlAtivaOuFalha();
        OpenDeliveryOrderResponse response = http.get()
                .uri(baseUrl + "/orders/{id}", pedidoExternoId)
                .retrieve()
                .body(OpenDeliveryOrderResponse.class);
        if (response == null) {
            throw new NotFoundException("Pedido " + pedidoExternoId + " não retornado pelo canal");
        }
        return OpenDeliveryResponseMapper.pedidoParaCanonico(response);
    }

    @Override
    public List<EventoBruto> consumirEventos(int max) {
        String baseUrl = baseUrlAtiva();
        if (baseUrl == null) {
            // Sem credencial cadastrada — silencioso, polling continua tentando.
            return List.of();
        }

        OpenDeliveryEventResponse[] eventos = http.get()
                .uri(baseUrl + "/events:polling?types=PLC,CFM,DSP,CON,CAN&max={max}", max)
                .retrieve()
                .body(OpenDeliveryEventResponse[].class);

        if (eventos == null || eventos.length == 0) {
            return List.of();
        }

        return java.util.Arrays.stream(eventos)
                .map(e -> {
                    String payload;
                    try {
                        payload = jackson.writeValueAsString(e);
                    } catch (Exception ex) {
                        log.warn("Falha serializando evento {} — usando JSON vazio", e.id(), ex);
                        payload = "{}";
                    }
                    return OpenDeliveryResponseMapper.eventoParaCanonico(e, payload);
                })
                .toList();
    }

    @Override
    public void acknowledgeEvento(String eventIdExterno) {
        String baseUrl = baseUrlAtivaOuFalha();
        http.post()
                .uri(baseUrl + "/events/acknowledgment")
                .body(List.of(Map.of("id", eventIdExterno)))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void confirmarPedido(String pedidoExternoId) {
        callOrderAction(pedidoExternoId, "confirm");
    }

    @Override
    public void despacharPedido(String pedidoExternoId) {
        callOrderAction(pedidoExternoId, "dispatch");
    }

    @Override
    public void concluirPedido(String pedidoExternoId) {
        callOrderAction(pedidoExternoId, "conclude");
    }

    @Override
    public void cancelarPedido(String pedidoExternoId, String motivo) {
        String baseUrl = baseUrlAtivaOuFalha();
        http.post()
                .uri(baseUrl + "/orders/{id}/cancellation", pedidoExternoId)
                .body(Map.of("reason", Optional.ofNullable(motivo).orElse("UNKNOWN")))
                .retrieve()
                .toBodilessEntity();
    }

    private void callOrderAction(String pedidoExternoId, String action) {
        String baseUrl = baseUrlAtivaOuFalha();
        http.post()
                .uri(baseUrl + "/orders/{id}/" + action, pedidoExternoId)
                .body(Collections.emptyMap())
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Pega a credencial ATIVA mais recente para este canal genérico. Em
     * produção o {@code uq_canais_credenciais_ativa} (V025) garante no
     * máximo uma por (canal, filial), mas pode haver várias filiais; o
     * sort por {@code createdAt} desc preserva o comportamento esperado
     * de rotação (a credencial nova ganha).
     */
    private String baseUrlAtiva() {
        return credenciais.listarPorCanal(CanalTipo.OPEN_DELIVERY_GENERICO).stream()
                .filter(CredencialCanal::ativa)
                .sorted(Comparator.comparing(CredencialCanal::createdAt).reversed())
                .map(c -> c.baseUrlOpt().orElse(null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String baseUrlAtivaOuFalha() {
        String url = baseUrlAtiva();
        if (url == null) {
            throw new NotFoundException("Nenhuma credencial Open Delivery genérica ativa com baseUrl");
        }
        return url;
    }
}
