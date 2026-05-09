package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferenciaTest {

    private final UUID origem = UUID.randomUUID();
    private final UUID destino = UUID.randomUUID();
    private final UUID solicitante = UUID.randomUUID();
    private final UUID gerente = UUID.randomUUID();
    private final UUID operador = UUID.randomUUID();
    private final UUID conferente = UUID.randomUUID();
    private final UUID insumoA = UUID.randomUUID();
    private final UUID insumoB = UUID.randomUUID();
    private final UUID unidade = UUID.randomUUID();
    private final Instant t0 = Instant.parse("2026-05-08T10:00:00Z");

    private List<ItemTransferencia> doisItens() {
        return List.of(
                ItemTransferencia.novo(insumoA, unidade, new BigDecimal("10")),
                ItemTransferencia.novo(insumoB, unidade, new BigDecimal("5")));
    }

    @Test
    void solicitar_ficaEmStatusSolicitada() {
        var t = Transferencia.solicitar(origem, destino, solicitante, doisItens(), null, t0);

        assertThat(t.status()).isEqualTo(StatusTransferencia.SOLICITADA);
        assertThat(t.solicitadoPor()).isEqualTo(solicitante);
        assertThat(t.dataSolicitacao()).isEqualTo(t0);
        assertThat(t.itens()).hasSize(2);
    }

    @Test
    void solicitar_filiaisIguais_lancaValidacao() {
        assertThatThrownBy(() -> Transferencia.solicitar(origem, origem, solicitante, doisItens(), null, t0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("distintas");
    }

    @Test
    void solicitar_insumoDuplicado_lancaValidacao() {
        var i1 = ItemTransferencia.novo(insumoA, unidade, new BigDecimal("1"));
        var i2 = ItemTransferencia.novo(insumoA, unidade, new BigDecimal("2"));
        assertThatThrownBy(() -> Transferencia.solicitar(origem, destino, solicitante, List.of(i1, i2), null, t0))
                .hasMessageContaining("duplicado");
    }

    @Test
    void solicitar_semItens_lancaValidacao() {
        assertThatThrownBy(() -> Transferencia.solicitar(origem, destino, solicitante, List.of(), null, t0))
                .hasMessageContaining("ao menos um item");
    }

    @Test
    void aprovar_apenasFromSolicitada() {
        var t = Transferencia.solicitar(origem, destino, solicitante, doisItens(), null, t0);
        t.aprovar(gerente, t0.plusSeconds(60));
        assertThat(t.status()).isEqualTo(StatusTransferencia.APROVADA);
        assertThat(t.aprovadoPorOpt()).contains(gerente);

        // Re-aprovar deve falhar
        assertThatThrownBy(() -> t.aprovar(gerente, t0.plusSeconds(120)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("APROVADA");
    }

    @Test
    void registrarEnvio_apenasFromAprovada() {
        var t = Transferencia.solicitar(origem, destino, solicitante, doisItens(), null, t0);
        UUID movSaida = UUID.randomUUID();

        // SOLICITADA → enviar deve falhar
        assertThatThrownBy(() -> t.registrarEnvio(operador, movSaida, t0))
                .isInstanceOf(BusinessRuleException.class);

        t.aprovar(gerente, t0.plusSeconds(60));
        t.registrarEnvio(operador, movSaida, t0.plusSeconds(120));

        assertThat(t.status()).isEqualTo(StatusTransferencia.EM_TRANSITO);
        assertThat(t.movSaidaIdOpt()).contains(movSaida);
    }

    @Test
    void registrarRecebimento_apenasFromEmTransito() {
        var t = Transferencia.solicitar(origem, destino, solicitante, doisItens(), null, t0);
        t.aprovar(gerente, t0.plusSeconds(60));
        t.registrarEnvio(operador, UUID.randomUUID(), t0.plusSeconds(120));

        UUID itemAId = t.itens().get(0).id();
        UUID itemBId = t.itens().get(1).id();
        UUID movEntrada = UUID.randomUUID();

        t.registrarRecebimento(conferente,
                Map.of(itemAId, new BigDecimal("10"), itemBId, new BigDecimal("4")),
                movEntrada, t0.plusSeconds(180));

        assertThat(t.status()).isEqualTo(StatusTransferencia.RECEBIDA);
        assertThat(t.movEntradaIdOpt()).contains(movEntrada);
        assertThat(t.itens().get(0).quantidadeRecebidaOpt()).contains(new BigDecimal("10"));
        assertThat(t.itens().get(1).quantidadeRecebidaOpt()).contains(new BigDecimal("4"));
        assertThat(t.itensComDivergencia()).hasSize(1);  // só item B teve divergência (4 vs 5)
    }

    @Test
    void cancelar_apenasFromSolicitadaOuAprovada() {
        var t = Transferencia.solicitar(origem, destino, solicitante, doisItens(), null, t0);
        t.cancelar("erro do solicitante", t0.plusSeconds(30));
        assertThat(t.status()).isEqualTo(StatusTransferencia.CANCELADA);
        assertThat(t.motivoCancelamentoOpt()).contains("erro do solicitante");

        // Outra transferência: cancela após aprovar (ainda permitido)
        var t2 = Transferencia.solicitar(origem, destino, solicitante, doisItens(), null, t0);
        t2.aprovar(gerente, t0.plusSeconds(30));
        t2.cancelar("desistência", t0.plusSeconds(60));
        assertThat(t2.status()).isEqualTo(StatusTransferencia.CANCELADA);
    }

    @Test
    void cancelar_apos_emTransito_falha() {
        var t = Transferencia.solicitar(origem, destino, solicitante, doisItens(), null, t0);
        t.aprovar(gerente, t0.plusSeconds(30));
        t.registrarEnvio(operador, UUID.randomUUID(), t0.plusSeconds(60));

        assertThatThrownBy(() -> t.cancelar("tarde demais", t0.plusSeconds(90)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("EM_TRANSITO");
    }

    @Test
    void cancelar_motivoVazio_lancaValidacao() {
        var t = Transferencia.solicitar(origem, destino, solicitante, doisItens(), null, t0);
        assertThatThrownBy(() -> t.cancelar("  ", t0))
                .isInstanceOf(ValidationException.class);
    }
}
