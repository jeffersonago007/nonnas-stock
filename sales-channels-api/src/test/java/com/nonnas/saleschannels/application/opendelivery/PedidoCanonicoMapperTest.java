package com.nonnas.saleschannels.application.opendelivery;

import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.CredencialCanalId;
import com.nonnas.saleschannels.domain.PedidoCanal;
import com.nonnas.saleschannels.domain.StatusPedidoCanal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PedidoCanonicoMapperTest {

    private static final Instant T0 = Instant.parse("2026-05-15T18:00:00Z");
    private static final UUID FILIAL = UUID.randomUUID();
    private static final CredencialCanalId CRED = CredencialCanalId.generate();

    @Test
    void mapeiaCanonicalCompletoEmDominio() {
        OpenDeliveryItem item1 = new OpenDeliveryItem(
                "i1", 1, "PIZZA-MARG", "Pizza Margherita",
                new BigDecimal("1"), OpenDeliveryUnit.UN,
                new OpenDeliveryPrice(new BigDecimal("49.90"), "BRL"),
                new OpenDeliveryPrice(new BigDecimal("49.90"), "BRL"),
                "Sem cebola", List.of());
        OpenDeliveryItem item2 = new OpenDeliveryItem(
                "i2", 2, "COCA-LATA", "Coca-Cola Lata",
                new BigDecimal("2"), OpenDeliveryUnit.UN,
                new OpenDeliveryPrice(new BigDecimal("6.00"), "BRL"),
                new OpenDeliveryPrice(new BigDecimal("12.00"), "BRL"),
                null, List.of());

        PedidoVendaCanonico canonico = new PedidoVendaCanonico(
                "ifood-order-abc",
                "#A1B2",
                OpenDeliveryOrderType.DELIVERY,
                "iFood",
                T0,
                new OpenDeliveryMerchant("merchant-mooca", "Nonnas Mooca"),
                new OpenDeliveryCustomer("cust-1", "Maria", "+5511999990000"),
                List.of(item1, item2),
                new OpenDeliveryTotal(
                        new BigDecimal("61.90"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("61.90"), "BRL"),
                null);

        PedidoCanal pedido = PedidoCanonicoMapper.paraDominio(
                canonico, CanalTipo.IFOOD, FILIAL, CRED, T0);

        assertThat(pedido.status()).isEqualTo(StatusPedidoCanal.RECEBIDO);
        assertThat(pedido.pedidoExternoId()).isEqualTo("ifood-order-abc");
        assertThat(pedido.displayIdOpt()).contains("#A1B2");
        assertThat(pedido.canalTipo()).isEqualTo(CanalTipo.IFOOD);
        assertThat(pedido.filialId()).isEqualTo(FILIAL);
        assertThat(pedido.valorTotal()).isEqualByComparingTo("61.90");
        assertThat(pedido.moeda()).isEqualTo("BRL");
        assertThat(pedido.clienteNomeOpt()).contains("Maria");
        assertThat(pedido.clienteTelefoneOpt()).contains("+5511999990000");
        assertThat(pedido.itens()).hasSize(2);
        assertThat(pedido.itens().get(0).sequencia()).isEqualTo(1);
        assertThat(pedido.itens().get(0).externalCodeOpt()).contains("PIZZA-MARG");
        assertThat(pedido.itens().get(0).observacaoOpt()).contains("Sem cebola");
        assertThat(pedido.itens().get(1).precoTotal()).isEqualByComparingTo("12.00");
    }

    @Test
    void caiParaSomaDeItensQuandoTotalAusente() {
        OpenDeliveryItem item = new OpenDeliveryItem(
                "i1", 1, "X", "Item",
                new BigDecimal("3"), OpenDeliveryUnit.UN,
                new OpenDeliveryPrice(new BigDecimal("5.00"), "BRL"),
                new OpenDeliveryPrice(new BigDecimal("15.00"), "BRL"),
                null, List.of());
        PedidoVendaCanonico canonico = new PedidoVendaCanonico(
                "x", "y", OpenDeliveryOrderType.DELIVERY, "iFood", T0,
                new OpenDeliveryMerchant("m", "M"), null,
                List.of(item), null, null);

        PedidoCanal pedido = PedidoCanonicoMapper.paraDominio(
                canonico, CanalTipo.IFOOD, FILIAL, CRED, T0);

        assertThat(pedido.valorTotal()).isEqualByComparingTo("15.00");
        assertThat(pedido.moeda()).isEqualTo("BRL"); // do unitPrice do item
    }

    @Test
    void caiParaBRLQuandoNadaTemMoeda() {
        OpenDeliveryItem item = new OpenDeliveryItem(
                "i1", 1, "X", "Item",
                new BigDecimal("1"), OpenDeliveryUnit.UN,
                null, null, null, List.of());
        PedidoVendaCanonico canonico = new PedidoVendaCanonico(
                "x", null, OpenDeliveryOrderType.DELIVERY, null, T0,
                new OpenDeliveryMerchant("m", "M"), null,
                List.of(item), null, null);

        PedidoCanal pedido = PedidoCanonicoMapper.paraDominio(
                canonico, CanalTipo.OPEN_DELIVERY_GENERICO, FILIAL, CRED, T0);

        assertThat(pedido.moeda()).isEqualTo("BRL");
        assertThat(pedido.valorTotal()).isEqualByComparingTo("0");
    }

    @Test
    void usaIndiceDaListaQuandoIndexZero() {
        OpenDeliveryItem item = new OpenDeliveryItem(
                "i1", 0, null, "Item sem index",
                new BigDecimal("1"), OpenDeliveryUnit.UN,
                new OpenDeliveryPrice(BigDecimal.ONE, "BRL"),
                new OpenDeliveryPrice(BigDecimal.ONE, "BRL"),
                null, List.of());
        PedidoVendaCanonico canonico = new PedidoVendaCanonico(
                "x", null, OpenDeliveryOrderType.DELIVERY, null, T0,
                new OpenDeliveryMerchant("m", "M"), null,
                List.of(item),
                new OpenDeliveryTotal(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, "BRL"),
                null);

        PedidoCanal pedido = PedidoCanonicoMapper.paraDominio(
                canonico, CanalTipo.IFOOD, FILIAL, CRED, T0);

        assertThat(pedido.itens().get(0).sequencia()).isEqualTo(1);
    }
}
