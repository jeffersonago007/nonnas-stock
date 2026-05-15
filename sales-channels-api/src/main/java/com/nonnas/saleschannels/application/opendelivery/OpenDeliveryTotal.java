package com.nonnas.saleschannels.application.opendelivery;

import java.math.BigDecimal;

/**
 * Total do pedido — Open Delivery v1.0.1 (subset).
 *
 * <p>{@code orderAmount} é o valor final cobrado do cliente — soma dos
 * itens menos descontos mais taxas. Para o POC só usamos este campo;
 * a decomposição vira útil quando integrarmos com financeiro.
 */
public record OpenDeliveryTotal(
        BigDecimal itemsPrice,
        BigDecimal otherFees,
        BigDecimal discount,
        BigDecimal orderAmount,
        String currency
) {}
