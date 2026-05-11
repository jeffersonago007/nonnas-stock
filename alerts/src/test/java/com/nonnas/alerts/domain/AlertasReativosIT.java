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
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do master doc T07: cria config "estoque mínimo para Mussarela na
 * Filial Centro", lança saídas até cruzar, valida que AlertaDisparado é criado.
 *
 * <p>Usamos {@code ESTOQUE_MINIMO_ABSOLUTO} no IT porque PERCENTUAL exigiria
 * cadeia completa de catalog (CategoriaInsumo + UnidadeMedida + Insumo +
 * InsumoFilial). A semântica do disparo é equivalente — PERCENTUAL é coberto
 * em {@link AvaliadorAlertasServiceTest} com mock de {@code InsumoFilialRepository}.
 */
class AlertasReativosIT extends AbstractAlertsIntegrationTest {

    @Autowired private CriarAlertaConfigUseCase criarConfig;
    @Autowired private ListarAlertasDisparadosUseCase listarDisparados;
    @Autowired private RegistrarEntradaManualUseCase entrada;
    @Autowired private RegistrarSaidaManualUseCase saida;

    @Test
    void estoqueMinimo_disparaQuandoSaldoCruzaThreshold_paraInsumoNaFilialEspecifica() {
        UUID mussarela = UUID.randomUUID();
        UUID centro = UUID.randomUUID();
        UUID kg = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();

        // Config: alarma quando saldo < 20kg de Mussarela na filial Centro
        criarConfig.execute(new CriarAlertaConfigUseCase.Comando(
                TipoAlerta.ESTOQUE_MINIMO_ABSOLUTO, mussarela, centro,
                new BigDecimal("20"), 0, "estoque mínimo Mussarela Centro"));

        // Entrada: 50kg na filial Centro
        entrada.execute(new RegistrarEntradaManualUseCase.Comando(
                centro, usuario, mussarela, null, null, "M-1",
                null, LocalDate.parse("2027-01-01"), new BigDecimal("32.50"),
                kg, new BigDecimal("50"), new BigDecimal("50"),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null));

        // Saldo 50kg — sem alerta para ESTE insumo/filial
        var ativos1 = listarDisparados.execute(filtroPorEscopo(mussarela, centro), 0, 50);
        assertThat(ativos1).isEmpty();

        // Saída de 35kg → saldo 15kg (cruza threshold de 20kg)
        saida.execute(new RegistrarSaidaManualUseCase.Comando(
                centro, usuario, mussarela, kg, new BigDecimal("35"),
                TipoMovimentacao.SAIDA_VENDA, null, null, null));

        var ativos2 = listarDisparados.execute(filtroPorEscopo(mussarela, centro), 0, 50);
        assertThat(ativos2).hasSize(1);
        assertThat(ativos2.get(0).tipo()).isEqualTo(TipoAlerta.ESTOQUE_MINIMO_ABSOLUTO);
        assertThat(ativos2.get(0).insumoId()).isEqualTo(mussarela);
        assertThat(ativos2.get(0).filialId()).isEqualTo(centro);
        assertThat(ativos2.get(0).status()).isEqualTo(StatusAlerta.ATIVO);
    }

    @Test
    void rupturaAlerta_disparaQuandoSaldoNegativo() {
        UUID insumo = UUID.randomUUID();
        UUID filial = UUID.randomUUID();
        UUID kg = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();

        // Config global de RUPTURA (sem escopo)
        criarConfig.execute(new CriarAlertaConfigUseCase.Comando(
                TipoAlerta.RUPTURA, null, null, null, 0, "ruptura global"));

        // Entrada de 5kg
        entrada.execute(new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumo, null, null, "L-1",
                null, LocalDate.parse("2027-01-01"), BigDecimal.TEN,
                kg, new BigDecimal("5"), new BigDecimal("5"),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null));

        // Saída de 10kg → saldo -5 (gera negativo, RUPTURA)
        saida.execute(new RegistrarSaidaManualUseCase.Comando(
                filial, usuario, insumo, kg, new BigDecimal("10"),
                TipoMovimentacao.SAIDA_VENDA, null, null, null));

        var ativos = listarDisparados.execute(filtroPorEscopo(insumo, filial), 0, 50);
        assertThat(ativos).hasSize(1);
        assertThat(ativos.get(0).insumoId()).isEqualTo(insumo);
        assertThat(ativos.get(0).tipo()).isEqualTo(TipoAlerta.RUPTURA);
    }

    @Test
    void autoResolucao_quandoSaldoVoltaAcimaDoThreshold() {
        UUID insumo = UUID.randomUUID();
        UUID filial = UUID.randomUUID();
        UUID kg = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();

        criarConfig.execute(new CriarAlertaConfigUseCase.Comando(
                TipoAlerta.ESTOQUE_MINIMO_ABSOLUTO, insumo, filial,
                new BigDecimal("20"), 0, "min 20kg"));

        // Entra 30, sai 15 → saldo 15 < 20: dispara
        entrada.execute(new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumo, null, null, "L-A",
                null, LocalDate.parse("2027-01-01"), BigDecimal.TEN,
                kg, new BigDecimal("30"), new BigDecimal("30"),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null));
        saida.execute(new RegistrarSaidaManualUseCase.Comando(
                filial, usuario, insumo, kg, new BigDecimal("15"),
                TipoMovimentacao.SAIDA_VENDA, null, null, null));

        var ativos1 = listarDisparados.execute(filtroPorEscopo(insumo, filial), 0, 50);
        assertThat(ativos1).hasSize(1);

        // Entra mais 20 → saldo 35 > 20: auto-resolve
        entrada.execute(new RegistrarEntradaManualUseCase.Comando(
                filial, usuario, insumo, null, null, "L-B",
                null, LocalDate.parse("2027-02-01"), BigDecimal.TEN,
                kg, new BigDecimal("20"), new BigDecimal("20"),
                TipoMovimentacao.ENTRADA_AJUSTE, null, null, null));

        var ativos2 = listarDisparados.execute(filtroPorEscopo(insumo, filial), 0, 50);
        assertThat(ativos2).isEmpty();

        var resolvidos = listarDisparados.execute(
                new ListarAlertasDisparadosUseCase.Filtros(
                        StatusAlerta.RESOLVIDO_AUTO, filial, insumo, null, null, null), 0, 50);
        assertThat(resolvidos).hasSize(1);
    }

    private ListarAlertasDisparadosUseCase.Filtros filtroPorEscopo(UUID insumoId, UUID filialId) {
        return new ListarAlertasDisparadosUseCase.Filtros(
                StatusAlerta.ATIVO, filialId, insumoId, null, null, null);
    }

}
