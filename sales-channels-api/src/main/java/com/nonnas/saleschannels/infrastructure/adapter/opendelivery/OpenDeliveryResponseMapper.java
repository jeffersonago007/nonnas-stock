package com.nonnas.saleschannels.infrastructure.adapter.opendelivery;

import com.nonnas.saleschannels.application.opendelivery.EventoBruto;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryCustomer;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryFee;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryFeeReceiver;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryFeeType;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryItem;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryMerchant;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryOption;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryOrderType;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryPrice;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryTotal;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryUnit;
import com.nonnas.saleschannels.application.opendelivery.PedidoVendaCanonico;
import com.nonnas.saleschannels.domain.TipoEventoCanal;

import java.util.List;
import java.util.Optional;

/**
 * Converte respostas HTTP do canal (DTOs de wire) para o subset canônico
 * Open Delivery v1.0.1 ({@link PedidoVendaCanonico}, {@link EventoBruto}).
 *
 * <p>Stateless, sem deps de Spring — usado pelo adapter e testável isoladamente.
 */
final class OpenDeliveryResponseMapper {

    private OpenDeliveryResponseMapper() {}

    static EventoBruto eventoParaCanonico(OpenDeliveryEventResponse e, String payloadJson) {
        return new EventoBruto(
                e.id(),
                mapearTipoEvento(e.code()),
                e.orderId(),
                e.merchantId(),
                e.createdAt(),
                payloadJson);
    }

    static PedidoVendaCanonico pedidoParaCanonico(OpenDeliveryOrderResponse o) {
        return new PedidoVendaCanonico(
                o.id(),
                o.displayId(),
                mapearTipo(o.type()),
                o.salesChannel(),
                o.createdAt(),
                merchant(o.merchant()),
                customer(o.customer()),
                Optional.ofNullable(o.items()).orElse(List.of()).stream()
                        .map(OpenDeliveryResponseMapper::item)
                        .toList(),
                Optional.ofNullable(o.otherFees()).orElse(List.of()).stream()
                        .map(OpenDeliveryResponseMapper::fee)
                        .toList(),
                total(o.total()),
                o.extraInfo());
    }

    private static OpenDeliveryFee fee(OpenDeliveryOrderResponse.Fee f) {
        return new OpenDeliveryFee(
                f.name(),
                mapearFeeType(f.type()),
                mapearFeeReceiver(f.receivedBy()),
                price(f.price()),
                f.observation());
    }

    private static OpenDeliveryFeeType mapearFeeType(String type) {
        if (type == null) return OpenDeliveryFeeType.OTHER;
        return switch (type.toUpperCase()) {
            case "DELIVERY_FEE" -> OpenDeliveryFeeType.DELIVERY_FEE;
            case "SERVICE_FEE" -> OpenDeliveryFeeType.SERVICE_FEE;
            case "TIP" -> OpenDeliveryFeeType.TIP;
            default -> OpenDeliveryFeeType.OTHER;
        };
    }

    private static OpenDeliveryFeeReceiver mapearFeeReceiver(String receivedBy) {
        if (receivedBy == null) return OpenDeliveryFeeReceiver.OTHER;
        return switch (receivedBy.toUpperCase()) {
            case "MARKETPLACE" -> OpenDeliveryFeeReceiver.MARKETPLACE;
            case "MERCHANT" -> OpenDeliveryFeeReceiver.MERCHANT;
            case "LOGISTIC_SERVICES" -> OpenDeliveryFeeReceiver.LOGISTIC_SERVICES;
            default -> OpenDeliveryFeeReceiver.OTHER;
        };
    }

    private static OpenDeliveryMerchant merchant(OpenDeliveryOrderResponse.Merchant m) {
        return m == null ? null : new OpenDeliveryMerchant(m.id(), m.name());
    }

    private static OpenDeliveryCustomer customer(OpenDeliveryOrderResponse.Customer c) {
        return c == null ? null : new OpenDeliveryCustomer(c.id(), c.name(), c.phone());
    }

    private static OpenDeliveryItem item(OpenDeliveryOrderResponse.Item it) {
        return new OpenDeliveryItem(
                it.id(),
                it.index(),
                it.externalCode(),
                it.name(),
                it.quantity(),
                unit(it.unit()),
                price(it.unitPrice()),
                price(it.totalPrice()),
                it.observations(),
                Optional.ofNullable(it.options()).orElse(List.of()).stream()
                        .map(OpenDeliveryResponseMapper::option)
                        .toList());
    }

    private static OpenDeliveryOption option(OpenDeliveryOrderResponse.Option op) {
        return new OpenDeliveryOption(
                op.externalCode(),
                op.name(),
                op.quantity(),
                unit(op.unit()),
                price(op.unitPrice()));
    }

    private static OpenDeliveryPrice price(OpenDeliveryOrderResponse.Price p) {
        return p == null ? null : new OpenDeliveryPrice(p.value(), p.currency());
    }

    private static OpenDeliveryTotal total(OpenDeliveryOrderResponse.Total t) {
        return t == null ? null : new OpenDeliveryTotal(
                t.itemsPrice(), t.otherFees(), t.discount(), t.orderAmount(), t.currency());
    }

    private static OpenDeliveryOrderType mapearTipo(String type) {
        if (type == null) return OpenDeliveryOrderType.DELIVERY;
        return switch (type.toUpperCase()) {
            case "TAKEOUT" -> OpenDeliveryOrderType.TAKEOUT;
            case "INDOOR" -> OpenDeliveryOrderType.INDOOR;
            default -> OpenDeliveryOrderType.DELIVERY;
        };
    }

    private static OpenDeliveryUnit unit(String u) {
        if (u == null) return OpenDeliveryUnit.UN;
        return switch (u.toUpperCase()) {
            case "KG" -> OpenDeliveryUnit.KG;
            case "L" -> OpenDeliveryUnit.L;
            case "OZ" -> OpenDeliveryUnit.OZ;
            case "LB" -> OpenDeliveryUnit.LB;
            case "GAL" -> OpenDeliveryUnit.GAL;
            default -> OpenDeliveryUnit.UN;
        };
    }

    private static TipoEventoCanal mapearTipoEvento(String code) {
        if (code == null) return TipoEventoCanal.OUTRO;
        return switch (code.toUpperCase()) {
            case "PLC", "PLACED" -> TipoEventoCanal.PEDIDO_CRIADO;
            case "CFM", "CONFIRMED" -> TipoEventoCanal.PEDIDO_CONFIRMADO;
            case "DSP", "DISPATCHED" -> TipoEventoCanal.PEDIDO_DESPACHADO;
            case "CON", "CONCLUDED" -> TipoEventoCanal.PEDIDO_CONCLUIDO;
            case "CAN", "CANCELLED" -> TipoEventoCanal.PEDIDO_CANCELADO;
            default -> TipoEventoCanal.OUTRO;
        };
    }
}
