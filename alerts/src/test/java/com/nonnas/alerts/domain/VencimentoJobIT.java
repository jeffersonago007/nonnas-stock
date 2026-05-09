package com.nonnas.alerts.domain;

import com.nonnas.alerts.application.config.CriarAlertaConfigUseCase;
import com.nonnas.alerts.application.disparado.ListarAlertasDisparadosUseCase;
import com.nonnas.inventory.application.movimentacao.RegistrarEntradaManualUseCase;
import com.nonnas.inventory.application.movimentacao.RegistrarSaidaManualUseCase;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.alerts.testsupport.AbstractAlertsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VencimentoJobIT extends AbstractAlertsIntegrationTest {

    @Autowired private CriarAlertaConfigUseCase criarConfig;
    @Autowired private ListarAlertasDisparadosUseCase listarDisparados;
    @Autowired private AvaliadorAlertasService avaliador;
    @Autowired private RegistrarEntradaManualUseCase entrada;
    @Autowired private RegistrarSaidaManualUseCase saida;
    @Autowired private Clock clock;

    @Test
    void avaliarVencimentos_loteVencendoEm5Dias_disparaParaConfigCom7Dias() {
        UUID insumo = UUID.randomUUID();
        UUID filial = UUID.randomUUID();
        UUID kg = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();

        criarConfig.execute(new CriarAlertaConfigUseCase.Comando(
                TipoAlerta.VENCIMENTO_PROXIMO_DIAS, null, null,
                new BigDecimal("7"), 0, "vencimento 7 dias"));

        LocalDate hoje = LocalDate.now(clock.withZone(ZoneId.of("America/Sao_Paulo")));

        // Lote vencendo em 5 dias (dentro da janela)
        entrada.execute(new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumo, null, null, "L-VENC",
                null, hoje.plusDays(5), BigDecimal.TEN,
                kg, new BigDecimal("10"), new BigDecimal("10"),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null));
        // Lote vencendo em 30 dias (fora da janela)
        entrada.execute(new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumo, null, null, "L-OK",
                null, hoje.plusDays(30), BigDecimal.TEN,
                kg, new BigDecimal("5"), new BigDecimal("5"),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null));

        avaliador.avaliarVencimentos();

        var ativos = listarDisparados.execute(filtroPorEscopo(insumo, filial), 0, 50);
        assertThat(ativos).hasSize(1);
        assertThat(ativos.get(0).tipo()).isEqualTo(TipoAlerta.VENCIMENTO_PROXIMO_DIAS);
        assertThat(ativos.get(0).loteIdOpt()).isPresent();
    }

    @Test
    void avaliarVencimentos_idempotente_naoDuplicaSeJaAtivo() {
        UUID insumo = UUID.randomUUID();
        UUID filial = UUID.randomUUID();
        UUID kg = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();

        criarConfig.execute(new CriarAlertaConfigUseCase.Comando(
                TipoAlerta.VENCIMENTO_PROXIMO_DIAS, null, null,
                new BigDecimal("7"), 0, null));

        LocalDate hoje = LocalDate.now(clock.withZone(ZoneId.of("America/Sao_Paulo")));
        entrada.execute(new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumo, null, null, "L-X",
                null, hoje.plusDays(2), BigDecimal.TEN,
                kg, new BigDecimal("10"), new BigDecimal("10"),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null));

        avaliador.avaliarVencimentos();
        var ativos1 = listarDisparados.execute(filtroPorEscopo(insumo, filial), 0, 50);
        assertThat(ativos1).hasSize(1);

        // Segunda execução: idempotente, sem duplicação
        avaliador.avaliarVencimentos();
        var ativos2 = listarDisparados.execute(filtroPorEscopo(insumo, filial), 0, 50);
        assertThat(ativos2).hasSize(1);
        assertThat(ativos2.get(0).id()).isEqualTo(ativos1.get(0).id());
    }

    @Test
    void avaliarVencimentos_loteZerado_resolveAutoNaProximaMovimentacao() {
        UUID insumo = UUID.randomUUID();
        UUID filial = UUID.randomUUID();
        UUID kg = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();

        criarConfig.execute(new CriarAlertaConfigUseCase.Comando(
                TipoAlerta.VENCIMENTO_PROXIMO_DIAS, null, null,
                new BigDecimal("7"), 0, null));

        LocalDate hoje = LocalDate.now(clock.withZone(ZoneId.of("America/Sao_Paulo")));
        entrada.execute(new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumo, null, null, "L-ZERAR",
                null, hoje.plusDays(1), BigDecimal.TEN,
                kg, new BigDecimal("3"), new BigDecimal("3"),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null));

        avaliador.avaliarVencimentos();
        var ativosAntes = listarDisparados.execute(filtroVencimentoPorEscopo(insumo, filial), 0, 50);
        assertThat(ativosAntes).hasSize(1);

        // Saída zera o lote — listener auto-resolve alertas de vencimento do lote zerado
        saida.execute(new RegistrarSaidaManualUseCase.Comando(
                filial, usuario, insumo, kg, new BigDecimal("3"),
                TipoMovimentacao.SAIDA_VENDA, null, null, null));

        var ativosDepois = listarDisparados.execute(filtroVencimentoPorEscopo(insumo, filial), 0, 50);
        assertThat(ativosDepois).isEmpty();
    }

    private ListarAlertasDisparadosUseCase.Filtros filtroPorEscopo(UUID insumoId, UUID filialId) {
        return new ListarAlertasDisparadosUseCase.Filtros(
                StatusAlerta.ATIVO, filialId, insumoId, null, null, null);
    }

    private ListarAlertasDisparadosUseCase.Filtros filtroVencimentoPorEscopo(UUID insumoId, UUID filialId) {
        return new ListarAlertasDisparadosUseCase.Filtros(
                StatusAlerta.ATIVO, filialId, insumoId, TipoAlerta.VENCIMENTO_PROXIMO_DIAS, null, null);
    }
}
