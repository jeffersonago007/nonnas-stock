package com.nonnas.saleschannels.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.MovimentacaoId;
import com.nonnas.recipes.application.venda.RegistrarVendaSimuladaUseCase;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryCustomer;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryItem;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryMerchant;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryOrderType;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryPrice;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryTotal;
import com.nonnas.saleschannels.application.opendelivery.OpenDeliveryUnit;
import com.nonnas.saleschannels.application.opendelivery.PedidoVendaCanonico;
import com.nonnas.saleschannels.application.ports.CanalAdapter;
import com.nonnas.saleschannels.application.ports.CanalProdutoDeParaRepository;
import com.nonnas.saleschannels.application.ports.CredencialCanalRepository;
import com.nonnas.saleschannels.application.ports.EventoCanalRepository;
import com.nonnas.saleschannels.application.ports.PedidoCanalRepository;
import com.nonnas.saleschannels.domain.CanalProdutoDePara;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.CredencialCanal;
import com.nonnas.saleschannels.domain.EventoCanal;
import com.nonnas.saleschannels.domain.PedidoCanal;
import com.nonnas.saleschannels.domain.StatusPedidoCanal;
import com.nonnas.saleschannels.domain.TipoEventoCanal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessarPedidoCanalUseCaseTest {

    private static final Instant T0 = Instant.parse("2026-05-15T18:00:00Z");

    private EventoCanalRepository eventos;
    private PedidoCanalRepository pedidos;
    private CredencialCanalRepository credenciais;
    private CanalProdutoDeParaRepository deparas;
    private CanalAdapter adapter;
    private RegistrarVendaSimuladaUseCase vendaUseCase;
    private ProcessarPedidoCanalUseCase useCase;

    private UUID merchantFilial;
    private CredencialCanal credencial;

    @BeforeEach
    void setUp() {
        eventos = mock(EventoCanalRepository.class);
        pedidos = mock(PedidoCanalRepository.class);
        credenciais = mock(CredencialCanalRepository.class);
        deparas = mock(CanalProdutoDeParaRepository.class);
        adapter = mock(CanalAdapter.class);
        when(adapter.canal()).thenReturn(CanalTipo.OPEN_DELIVERY_GENERICO);
        vendaUseCase = mock(RegistrarVendaSimuladaUseCase.class);

        Clock clock = Clock.fixed(T0, ZoneOffset.UTC);
        useCase = new ProcessarPedidoCanalUseCase(
                eventos, pedidos, credenciais, deparas,
                List.of(adapter), vendaUseCase, new ObjectMapper(), clock);

        merchantFilial = UUID.randomUUID();
        credencial = CredencialCanal.nova(
                CanalTipo.OPEN_DELIVERY_GENERICO, merchantFilial,
                "merchant-test", "client", "AES:secret", "http://mock", null, T0);
    }

    private PedidoVendaCanonico canonicoCom2Itens() {
        OpenDeliveryPrice price = new OpenDeliveryPrice(new BigDecimal("10"), "BRL");
        return new PedidoVendaCanonico(
                "ord-1", "#1", OpenDeliveryOrderType.DELIVERY, "iFood", T0,
                new OpenDeliveryMerchant("merchant-test", "Nonnas"),
                new OpenDeliveryCustomer("c1", "Maria", "+55"),
                List.of(
                        new OpenDeliveryItem("i1", 1, "PIZZA-MARG", "Margherita",
                                BigDecimal.ONE, OpenDeliveryUnit.UN, price, price, null, List.of()),
                        new OpenDeliveryItem("i2", 2, "COCA-LATA", "Coca",
                                new BigDecimal("2"), OpenDeliveryUnit.UN, price, price, null, List.of())
                ),
                new OpenDeliveryTotal(new BigDecimal("20"), BigDecimal.ZERO,
                        BigDecimal.ZERO, new BigDecimal("20"), "BRL"),
                null);
    }

    @Test
    void eventoCriadoBuscaResolveBaixaEConfirma() {
        UUID eventoId = UUID.randomUUID();
        EventoCanal evento = EventoCanal.recebido(
                CanalTipo.OPEN_DELIVERY_GENERICO, "evt-1", TipoEventoCanal.PEDIDO_CRIADO,
                "ord-1", "{}", T0);
        when(eventos.findById(any())).thenReturn(Optional.of(evento));

        when(pedidos.findByCanalEPedidoExterno(CanalTipo.OPEN_DELIVERY_GENERICO, "ord-1"))
                .thenReturn(Optional.empty());

        when(adapter.buscarPedido("ord-1")).thenReturn(canonicoCom2Itens());
        when(credenciais.findAtivaByMerchantExterno(CanalTipo.OPEN_DELIVERY_GENERICO, "merchant-test"))
                .thenReturn(Optional.of(credencial));

        // pedido novo persistido — captura para próximas chamadas.
        when(pedidos.salvarNovo(any(), anyString(), anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(pedidos.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID prodPizza = UUID.randomUUID();
        UUID prodCoca = UUID.randomUUID();
        when(deparas.resolver(CanalTipo.OPEN_DELIVERY_GENERICO, "PIZZA-MARG", credencial.filialId()))
                .thenReturn(Optional.of(CanalProdutoDePara.novo(
                        CanalTipo.OPEN_DELIVERY_GENERICO, "PIZZA-MARG",
                        credencial.filialId(), prodPizza, null, T0)));
        when(deparas.resolver(CanalTipo.OPEN_DELIVERY_GENERICO, "COCA-LATA", credencial.filialId()))
                .thenReturn(Optional.of(CanalProdutoDePara.novo(
                        CanalTipo.OPEN_DELIVERY_GENERICO, "COCA-LATA",
                        credencial.filialId(), prodCoca, null, T0)));

        Movimentacao mov = mock(Movimentacao.class);
        when(mov.id()).thenReturn(MovimentacaoId.generate());
        when(vendaUseCase.execute(any())).thenReturn(mov);

        UUID usuarioSistema = UUID.randomUUID();
        PedidoCanal resultado = useCase.processarEventoNaoProcessado(eventoId, usuarioSistema);

        assertThat(resultado).isNotNull();
        assertThat(resultado.status()).isEqualTo(StatusPedidoCanal.CONCLUIDO);

        // venda chamada 2x (1 por item)
        ArgumentCaptor<RegistrarVendaSimuladaUseCase.Comando> captor =
                ArgumentCaptor.forClass(RegistrarVendaSimuladaUseCase.Comando.class);
        verify(vendaUseCase, times(2)).execute(captor.capture());
        assertThat(captor.getAllValues()).extracting(c -> c.produtoVendavelId())
                .containsExactlyInAnyOrder(prodPizza, prodCoca);

        // confirmação no canal
        verify(adapter).confirmarPedido("ord-1");

        // evento processado
        assertThat(evento.processadoEmOpt()).isPresent();
        verify(eventos).atualizar(eq(evento));
    }

    @Test
    void eventoJaProcessadoEhIgnorado() {
        EventoCanal evento = EventoCanal.recebido(
                CanalTipo.OPEN_DELIVERY_GENERICO, "evt-x", TipoEventoCanal.PEDIDO_CRIADO,
                "ord-x", "{}", T0);
        evento.marcarProcessado(T0);
        when(eventos.findById(any())).thenReturn(Optional.of(evento));

        PedidoCanal resultado = useCase.processarEventoNaoProcessado(UUID.randomUUID(), UUID.randomUUID());

        assertThat(resultado).isNull();
        verify(adapter, never()).buscarPedido(anyString());
        verify(vendaUseCase, never()).execute(any());
    }

    @Test
    void semDeParaFalhaEMarcaErroNoEvento() {
        EventoCanal evento = EventoCanal.recebido(
                CanalTipo.OPEN_DELIVERY_GENERICO, "evt-2", TipoEventoCanal.PEDIDO_CRIADO,
                "ord-2", "{}", T0);
        when(eventos.findById(any())).thenReturn(Optional.of(evento));
        when(pedidos.findByCanalEPedidoExterno(any(), anyString())).thenReturn(Optional.empty());
        when(adapter.buscarPedido("ord-2")).thenReturn(canonicoCom2Itens());
        when(credenciais.findAtivaByMerchantExterno(any(), anyString()))
                .thenReturn(Optional.of(credencial));
        when(pedidos.salvarNovo(any(), anyString(), anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(pedidos.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deparas.resolver(any(), anyString(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.processarEventoNaoProcessado(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Sem de-para");

        assertThat(evento.processadoEmOpt()).isPresent();
        assertThat(evento.erroOpt()).isPresent();
    }

    @Test
    void confirmacaoFalhandoNaoRevertEstoqueEDeixaConfirmadoEstoque() {
        UUID eventoId = UUID.randomUUID();
        EventoCanal evento = EventoCanal.recebido(
                CanalTipo.OPEN_DELIVERY_GENERICO, "evt-3", TipoEventoCanal.PEDIDO_CRIADO,
                "ord-3", "{}", T0);
        when(eventos.findById(any())).thenReturn(Optional.of(evento));
        when(pedidos.findByCanalEPedidoExterno(any(), anyString())).thenReturn(Optional.empty());

        // 1 item só para simplificar.
        OpenDeliveryPrice price = new OpenDeliveryPrice(BigDecimal.TEN, "BRL");
        PedidoVendaCanonico canonico = new PedidoVendaCanonico(
                "ord-3", "#3", OpenDeliveryOrderType.DELIVERY, "iFood", T0,
                new OpenDeliveryMerchant("merchant-test", "Nonnas"), null,
                List.of(new OpenDeliveryItem("i1", 1, "X", "Item", BigDecimal.ONE,
                        OpenDeliveryUnit.UN, price, price, null, List.of())),
                new OpenDeliveryTotal(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, "BRL"),
                null);
        when(adapter.buscarPedido("ord-3")).thenReturn(canonico);
        when(credenciais.findAtivaByMerchantExterno(any(), anyString()))
                .thenReturn(Optional.of(credencial));
        when(pedidos.salvarNovo(any(), anyString(), anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(pedidos.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID prodId = UUID.randomUUID();
        when(deparas.resolver(any(), anyString(), any()))
                .thenReturn(Optional.of(CanalProdutoDePara.novo(
                        CanalTipo.OPEN_DELIVERY_GENERICO, "X", credencial.filialId(),
                        prodId, null, T0)));

        Movimentacao mov = mock(Movimentacao.class);
        when(mov.id()).thenReturn(MovimentacaoId.generate());
        when(vendaUseCase.execute(any())).thenReturn(mov);

        // Confirmar falha — não propaga; pedido fica CONFIRMADO_ESTOQUE.
        org.mockito.Mockito.doThrow(new RuntimeException("canal indisponível"))
                .when(adapter).confirmarPedido("ord-3");

        PedidoCanal resultado = useCase.processarEventoNaoProcessado(eventoId, UUID.randomUUID());

        assertThat(resultado).isNotNull();
        assertThat(resultado.status()).isEqualTo(StatusPedidoCanal.CONFIRMADO_ESTOQUE);
        verify(vendaUseCase).execute(any());
    }

    @Test
    void eventoCancelamentoCancelaPedidoExistente() {
        EventoCanal evento = EventoCanal.recebido(
                CanalTipo.OPEN_DELIVERY_GENERICO, "evt-c", TipoEventoCanal.PEDIDO_CANCELADO,
                "ord-c", "{}", T0);
        when(eventos.findById(any())).thenReturn(Optional.of(evento));

        // pedido existente em RECEBIDO
        PedidoCanal pedido = PedidoCanal.recebido(
                CanalTipo.OPEN_DELIVERY_GENERICO, "ord-c", null,
                credencial.filialId(), credencial.id(), BigDecimal.ZERO, "BRL", null, null,
                List.of(com.nonnas.saleschannels.domain.ItemPedidoCanal.novo(
                        1, "X", "x", BigDecimal.ONE, "UN", BigDecimal.ONE, BigDecimal.ONE, null)),
                T0);
        when(pedidos.findByCanalEPedidoExterno(CanalTipo.OPEN_DELIVERY_GENERICO, "ord-c"))
                .thenReturn(Optional.of(pedido));
        when(pedidos.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        PedidoCanal resultado = useCase.processarEventoNaoProcessado(UUID.randomUUID(), UUID.randomUUID());

        assertThat(resultado).isNotNull();
        assertThat(resultado.status()).isEqualTo(StatusPedidoCanal.CANCELADO);
        verify(vendaUseCase, never()).execute(any());
    }
}
