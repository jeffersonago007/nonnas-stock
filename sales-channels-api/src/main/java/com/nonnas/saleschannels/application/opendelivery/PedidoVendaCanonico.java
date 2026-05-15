package com.nonnas.saleschannels.application.opendelivery;

import java.time.Instant;
import java.util.List;

/**
 * Pedido de venda canônico — subset Open Delivery v1.0.1 que o sistema
 * Nonnas reconhece. <strong>Todo</strong> {@code CanalAdapter} converte
 * o payload nativo do seu canal para este contrato; a partir daqui o
 * processamento é uniforme.
 *
 * <p>Mapeamento direto da spec (campos {@code id}, {@code displayId},
 * {@code type}, {@code merchant}, {@code items}, {@code total},
 * {@code customer}, {@code createdAt}, {@code extraInfo}).
 *
 * <p>Fora do POC (capturado em {@link OpenDeliveryOrderType} mas não
 * processado adiante):
 * <ul>
 *   <li>{@code payments} — método de pagamento não afeta baixa de estoque.</li>
 *   <li>{@code delivery} — informações de entregador / endereço de entrega.</li>
 *   <li>{@code schedule} — pedidos agendados.</li>
 *   <li>{@code indoor} — atendimento presencial em mesa.</li>
 * </ul>
 *
 * <p>Quando esses domínios entrarem (financeiro, logística, salão), basta
 * estender este record sem quebrar adapters existentes.
 *
 * @see <a href="https://abrasel-nacional.github.io/docs/versions/1.0.1/">Open Delivery v1.0.1 spec</a>
 */
public record PedidoVendaCanonico(
        String id,
        String displayId,
        OpenDeliveryOrderType type,
        String salesChannel,
        Instant createdAt,
        OpenDeliveryMerchant merchant,
        OpenDeliveryCustomer customer,
        List<OpenDeliveryItem> items,
        OpenDeliveryTotal total,
        String extraInfo
) {
    public PedidoVendaCanonico {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Pedido canônico precisa de ao menos 1 item");
        }
    }
}
