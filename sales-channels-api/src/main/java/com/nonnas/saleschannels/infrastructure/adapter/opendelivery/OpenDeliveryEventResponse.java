package com.nonnas.saleschannels.infrastructure.adapter.opendelivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Payload de um evento retornado por {@code GET /events:polling} (Open Delivery v1.0.1).
 * Campos extras do canal são ignorados (forward-compat).
 *
 * <p>{@code code} é o tipo de evento ("PLC", "CFM", "DSP", "CON", "CAN", ...).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDeliveryEventResponse(
        String id,
        String code,
        String orderId,
        String merchantId,
        Instant createdAt,
        String fullCode
) {}
