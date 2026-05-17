package com.nonnas.saleschannels.application.opendelivery;

/**
 * Tipo da taxa de {@code otherFees} — Open Delivery v1.0.1.
 *
 * <p>{@code OTHER} é uma extensão local para preservar valores quando o
 * canal envia um valor fora dos 3 tipos canônicos (forward-compat).
 */
public enum OpenDeliveryFeeType {
    DELIVERY_FEE,
    SERVICE_FEE,
    TIP,
    OTHER
}
