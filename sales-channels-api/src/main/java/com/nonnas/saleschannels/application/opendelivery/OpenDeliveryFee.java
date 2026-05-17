package com.nonnas.saleschannels.application.opendelivery;

/**
 * Item do array {@code otherFees} de um pedido Open Delivery v1.0.1.
 *
 * <p>Discriminado por {@link OpenDeliveryFeeType} ({@code DELIVERY_FEE},
 * {@code SERVICE_FEE}, {@code TIP}) — o agregador {@code total.otherFees}
 * é apenas a soma desses valores. O canônico interno preserva o array
 * detalhado porque taxa de serviço e taxa de entrega são exibidas
 * separadamente ao operador.
 */
public record OpenDeliveryFee(
        String name,
        OpenDeliveryFeeType type,
        OpenDeliveryFeeReceiver receivedBy,
        OpenDeliveryPrice price,
        String observation
) {}
