package com.nonnas.saleschannels.infrastructure.adapter.opendelivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Payload de um pedido retornado por {@code GET /orders/{id}} (Open Delivery v1.0.1).
 * Subset suficiente para converter em {@link com.nonnas.saleschannels.application.opendelivery.PedidoVendaCanonico}.
 * Campos extras do canal são ignorados (forward-compat).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDeliveryOrderResponse(
        String id,
        String displayId,
        String type,
        String salesChannel,
        Instant createdAt,
        Merchant merchant,
        Customer customer,
        List<Item> items,
        List<Fee> otherFees,
        Total total,
        String extraInfo
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Merchant(String id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Customer(String id, String name, String phone) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String id,
            int index,
            String externalCode,
            String name,
            BigDecimal quantity,
            String unit,
            Price unitPrice,
            Price totalPrice,
            String observations,
            List<Option> options
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Option(
            String externalCode,
            String name,
            BigDecimal quantity,
            String unit,
            Price unitPrice
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Price(BigDecimal value, String currency) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fee(
            String name,
            String type,
            String receivedBy,
            Price price,
            String observation
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Total(
            BigDecimal itemsPrice,
            BigDecimal otherFees,
            BigDecimal discount,
            BigDecimal orderAmount,
            String currency
    ) {}
}
