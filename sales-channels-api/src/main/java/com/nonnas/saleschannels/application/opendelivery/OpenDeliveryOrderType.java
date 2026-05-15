package com.nonnas.saleschannels.application.opendelivery;

/**
 * Tipo de pedido (Open Delivery v1.0.1 — campo {@code order.type}).
 * Para Nonnas, hoje só DELIVERY é relevante; TAKEOUT/INDOOR ficam mapeados
 * para preservar interoperabilidade quando o canal enviar.
 */
public enum OpenDeliveryOrderType {
    DELIVERY,
    TAKEOUT,
    INDOOR
}
