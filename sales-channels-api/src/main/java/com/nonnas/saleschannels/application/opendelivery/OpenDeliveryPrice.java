package com.nonnas.saleschannels.application.opendelivery;

import java.math.BigDecimal;

/**
 * Preço com moeda — Open Delivery v1.0.1 (campos {@code unitPrice}, {@code totalPrice}).
 */
public record OpenDeliveryPrice(BigDecimal value, String currency) {}
