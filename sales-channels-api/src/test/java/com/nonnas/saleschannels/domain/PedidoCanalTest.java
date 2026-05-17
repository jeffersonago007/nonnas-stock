package com.nonnas.saleschannels.domain;

import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedidoCanalTest {

    private static final Instant T0 = Instant.parse("2026-05-15T18:00:00Z");
    private static final Instant T1 = T0.plusSeconds(10);
    private static final Instant T2 = T0.plusSeconds(60);
    private static final UUID FILIAL = UUID.randomUUID();
    private static final CredencialCanalId CRED = CredencialCanalId.generate();

    private PedidoCanal pedidoNovo() {
        ItemPedidoCanal item = ItemPedidoCanal.novo(
                1, "PIZZA-MARG", "Pizza Margherita",
                new BigDecimal("1"), "UN",
                new BigDecimal("49.90"), new BigDecimal("49.90"),
                null);
        return PedidoCanal.recebido(
                CanalTipo.IFOOD, "ifood-order-123", "#A1B2",
                FILIAL, CRED,
                new BigDecimal("49.90"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("49.90"),
                "BRL",
                "Maria", "+5511999990000",
                List.of(item), T0);
    }

    @Test
    void nasceEmRecebido() {
        PedidoCanal p = pedidoNovo();
        assertThat(p.status()).isEqualTo(StatusPedidoCanal.RECEBIDO);
        assertThat(p.canalTipo()).isEqualTo(CanalTipo.IFOOD);
        assertThat(p.recebidoEm()).isEqualTo(T0);
        assertThat(p.itens()).hasSize(1);
        assertThat(p.movimentacaoIdOpt()).isEmpty();
    }

    @Test
    void transitaParaProcessamentoConfirmadoConcluido() {
        PedidoCanal p = pedidoNovo();
        p.iniciarProcessamento(T1);
        assertThat(p.status()).isEqualTo(StatusPedidoCanal.EM_PROCESSAMENTO);

        UUID movId = UUID.randomUUID();
        p.confirmarEstoque(movId, T2);
        assertThat(p.status()).isEqualTo(StatusPedidoCanal.CONFIRMADO_ESTOQUE);
        assertThat(p.movimentacaoIdOpt()).contains(movId);

        p.concluir(T2.plusSeconds(120));
        assertThat(p.status()).isEqualTo(StatusPedidoCanal.CONCLUIDO);
        assertThat(p.concluidoEmOpt()).isPresent();
    }

    @Test
    void naoConfirmaSemPassarPorProcessamento() {
        PedidoCanal p = pedidoNovo();
        assertThatThrownBy(() -> p.confirmarEstoque(UUID.randomUUID(), T1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("EM_PROCESSAMENTO");
    }

    @Test
    void naoConcluiSemConfirmarEstoque() {
        PedidoCanal p = pedidoNovo();
        p.iniciarProcessamento(T1);
        assertThatThrownBy(() -> p.concluir(T2))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CONFIRMADO_ESTOQUE");
    }

    @Test
    void marcarFalhaPermitidoEmRecebidoOuProcessamento() {
        PedidoCanal p1 = pedidoNovo();
        p1.marcarFalha("erro X", T1);
        assertThat(p1.status()).isEqualTo(StatusPedidoCanal.FALHA);
        assertThat(p1.erroProcessamentoOpt()).contains("erro X");

        PedidoCanal p2 = pedidoNovo();
        p2.iniciarProcessamento(T1);
        p2.marcarFalha("erro Y", T2);
        assertThat(p2.status()).isEqualTo(StatusPedidoCanal.FALHA);
    }

    @Test
    void marcarFalhaProibidoDepoisDeConcluido() {
        PedidoCanal p = pedidoNovo();
        p.iniciarProcessamento(T1);
        p.confirmarEstoque(UUID.randomUUID(), T2);
        p.concluir(T2.plusSeconds(60));
        assertThatThrownBy(() -> p.marcarFalha("tarde", T2.plusSeconds(120)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancelarPermitidoExcetoEmConcluidoOuJaCancelado() {
        PedidoCanal p = pedidoNovo();
        p.cancelar(T1);
        assertThat(p.status()).isEqualTo(StatusPedidoCanal.CANCELADO);
        assertThat(p.canceladoEmOpt()).isPresent();

        // Cancelar de novo bate em CANCELADO → não permitido.
        assertThatThrownBy(() -> p.cancelar(T2)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancelarProibidoEmConcluido() {
        PedidoCanal p = pedidoNovo();
        p.iniciarProcessamento(T1);
        p.confirmarEstoque(UUID.randomUUID(), T2);
        p.concluir(T2.plusSeconds(60));
        assertThatThrownBy(() -> p.cancelar(T2.plusSeconds(120)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void pedidoSemItensRejeita() {
        assertThatThrownBy(() -> PedidoCanal.recebido(
                CanalTipo.IFOOD, "x", null, FILIAL, CRED,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "BRL", null, null,
                List.of(), T0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("1 item");
    }
}
