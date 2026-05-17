package com.nonnas.saleschannels.infrastructure.adapter.opendelivery;

import com.nonnas.saleschannels.application.opendelivery.EventoBruto;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryFeeReceiver;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryFeeType;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryOrderType;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryUnit;
import com.nonnas.saleschannels.application.opendelivery.PedidoVendaCanonico;
import com.nonnas.saleschannels.domain.TipoEventoCanal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenDeliveryResponseMapperTest {

    private static final Instant T0 = Instant.parse("2026-05-15T18:00:00Z");

    @Test
    void eventoMapeiaCodigosOpenDelivery() {
        EventoBruto bruto = OpenDeliveryResponseMapper.eventoParaCanonico(
                new OpenDeliveryEventResponse("evt-1", "PLC", "ord-1", "merch-1", T0, "PLC-1"),
                "{\"id\":\"evt-1\"}");

        assertThat(bruto.tipoEvento()).isEqualTo(TipoEventoCanal.PEDIDO_CRIADO);
        assertThat(bruto.pedidoExternoId()).isEqualTo("ord-1");
        assertThat(bruto.merchantExternoId()).isEqualTo("merch-1");
        assertThat(bruto.payloadJson()).contains("evt-1");
    }

    @Test
    void eventoComCodigoLongFormatTambemMapeia() {
        EventoBruto bruto = OpenDeliveryResponseMapper.eventoParaCanonico(
                new OpenDeliveryEventResponse("x", "CONFIRMED", "o", null, T0, null),
                "{}");
        assertThat(bruto.tipoEvento()).isEqualTo(TipoEventoCanal.PEDIDO_CONFIRMADO);
    }

    @Test
    void eventoDesconhecidoCaiEmOUTRO() {
        EventoBruto bruto = OpenDeliveryResponseMapper.eventoParaCanonico(
                new OpenDeliveryEventResponse("x", "ALGUM_TIPO_NOVO", "o", null, T0, null),
                "{}");
        assertThat(bruto.tipoEvento()).isEqualTo(TipoEventoCanal.OUTRO);
    }

    @Test
    void pedidoMapeiaCompletoComItensEOptions() {
        OpenDeliveryOrderResponse.Price unit = new OpenDeliveryOrderResponse.Price(new BigDecimal("49.90"), "BRL");
        OpenDeliveryOrderResponse response = new OpenDeliveryOrderResponse(
                "ord-xyz", "#A1", "DELIVERY", "iFood", T0,
                new OpenDeliveryOrderResponse.Merchant("m1", "Nonnas"),
                new OpenDeliveryOrderResponse.Customer("c1", "Maria", "+5511..."),
                List.of(new OpenDeliveryOrderResponse.Item(
                        "i1", 1, "PIZZA-MARG", "Margherita",
                        BigDecimal.ONE, "UN", unit, unit, "sem cebola",
                        List.of(new OpenDeliveryOrderResponse.Option(
                                "EXTRA-Q", "Queijo extra", BigDecimal.ONE, "UN", unit)))),
                null,
                new OpenDeliveryOrderResponse.Total(
                        new BigDecimal("49.90"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("49.90"), "BRL"),
                "obs");

        PedidoVendaCanonico canonico = OpenDeliveryResponseMapper.pedidoParaCanonico(response);

        assertThat(canonico.id()).isEqualTo("ord-xyz");
        assertThat(canonico.type()).isEqualTo(OpenDeliveryOrderType.DELIVERY);
        assertThat(canonico.merchant().id()).isEqualTo("m1");
        assertThat(canonico.items()).hasSize(1);
        assertThat(canonico.items().get(0).externalCode()).isEqualTo("PIZZA-MARG");
        assertThat(canonico.items().get(0).unit()).isEqualTo(OpenDeliveryUnit.UN);
        assertThat(canonico.items().get(0).options()).hasSize(1);
        assertThat(canonico.items().get(0).options().get(0).name()).isEqualTo("Queijo extra");
        assertThat(canonico.total().orderAmount()).isEqualByComparingTo("49.90");
        assertThat(canonico.otherFees()).isEmpty();
    }

    @Test
    void unidadeDesconhecidaCaiEmUN() {
        OpenDeliveryOrderResponse response = new OpenDeliveryOrderResponse(
                "x", null, null, null, T0, null, null,
                List.of(new OpenDeliveryOrderResponse.Item(
                        "i", 0, "X", "Item", BigDecimal.ONE,
                        "XYZ_NAO_EXISTE", null, null, null, null)),
                null, null, null);
        PedidoVendaCanonico canonico = OpenDeliveryResponseMapper.pedidoParaCanonico(response);
        assertThat(canonico.items().get(0).unit()).isEqualTo(OpenDeliveryUnit.UN);
    }

    @Test
    void otherFeesMapeiaTipoEReceiverComFallback() {
        OpenDeliveryOrderResponse.Price preco = new OpenDeliveryOrderResponse.Price(new BigDecimal("8.90"), "BRL");
        OpenDeliveryOrderResponse response = new OpenDeliveryOrderResponse(
                "ord-1", null, "DELIVERY", null, T0, null, null,
                List.of(new OpenDeliveryOrderResponse.Item(
                        "i", 1, "X", "Item", BigDecimal.ONE,
                        "UN", preco, preco, null, null)),
                List.of(
                        new OpenDeliveryOrderResponse.Fee("Entrega", "DELIVERY_FEE", "LOGISTIC_SERVICES", preco, null),
                        new OpenDeliveryOrderResponse.Fee("Serviço", "SERVICE_FEE", "MARKETPLACE", preco, null),
                        new OpenDeliveryOrderResponse.Fee("Gorjeta", "TIP", "MERCHANT", preco, null),
                        new OpenDeliveryOrderResponse.Fee("Desconhecida", "FUTURA_X", null, preco, null)),
                null, null);

        PedidoVendaCanonico canonico = OpenDeliveryResponseMapper.pedidoParaCanonico(response);

        assertThat(canonico.otherFees()).hasSize(4);
        assertThat(canonico.otherFees().get(0).type()).isEqualTo(OpenDeliveryFeeType.DELIVERY_FEE);
        assertThat(canonico.otherFees().get(0).receivedBy()).isEqualTo(OpenDeliveryFeeReceiver.LOGISTIC_SERVICES);
        assertThat(canonico.otherFees().get(1).type()).isEqualTo(OpenDeliveryFeeType.SERVICE_FEE);
        assertThat(canonico.otherFees().get(2).type()).isEqualTo(OpenDeliveryFeeType.TIP);
        assertThat(canonico.otherFees().get(3).type()).isEqualTo(OpenDeliveryFeeType.OTHER);
        assertThat(canonico.otherFees().get(3).receivedBy()).isEqualTo(OpenDeliveryFeeReceiver.OTHER);
    }
}
