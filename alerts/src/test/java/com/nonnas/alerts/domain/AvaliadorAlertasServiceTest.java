package com.nonnas.alerts.domain;

import com.nonnas.alerts.application.ports.AlertaConfigRepository;
import com.nonnas.alerts.application.ports.AlertaDisparadoRepository;
import com.nonnas.catalog.application.ports.InsumoFilialRepository;
import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AvaliadorAlertasServiceTest {

    private final AlertaConfigRepository configRepo = mock(AlertaConfigRepository.class);
    private final AlertaDisparadoRepository disparadoRepo = mock(AlertaDisparadoRepository.class);
    private final SaldoLoteRepository saldoRepo = mock(SaldoLoteRepository.class);
    private final InsumoFilialRepository insumoFilialRepo = mock(InsumoFilialRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-08T10:00:00Z"), ZoneOffset.UTC);

    private final AvaliadorAlertasService avaliador = new AvaliadorAlertasService(
            configRepo, disparadoRepo, saldoRepo, insumoFilialRepo, clock);

    private final UUID insumo = UUID.randomUUID();
    private final UUID filial = UUID.randomUUID();

    @Test
    void melhorMatch_priorizaConfigMaisEspecifica() {
        var global = AlertaConfig.novo(TipoAlerta.RUPTURA, null, null, null, 0, null, clock.instant());
        var soInsumo = AlertaConfig.novo(TipoAlerta.RUPTURA, insumo, null, null, 0, null, clock.instant());
        var ambos = AlertaConfig.novo(TipoAlerta.RUPTURA, insumo, filial, null, 0, null, clock.instant());

        var escolhida = avaliador.melhorMatch(List.of(global, soInsumo, ambos), insumo, filial);
        assertThat(escolhida).isEqualTo(ambos);

        var escolhida2 = avaliador.melhorMatch(List.of(global, soInsumo), insumo, filial);
        assertThat(escolhida2).isEqualTo(soInsumo);

        var escolhida3 = avaliador.melhorMatch(List.of(global), insumo, filial);
        assertThat(escolhida3).isEqualTo(global);
    }

    @Test
    void melhorMatch_empateDeEspecificidade_resolvePorPrioridade() {
        var p0 = AlertaConfig.novo(TipoAlerta.RUPTURA, null, null, null, 0, "p0", clock.instant());
        var p10 = AlertaConfig.novo(TipoAlerta.RUPTURA, null, null, null, 10, "p10", clock.instant());

        var escolhida = avaliador.melhorMatch(List.of(p0, p10), insumo, filial);
        assertThat(escolhida.observacaoOpt()).contains("p10");
    }

    @Test
    void melhorMatch_filtraEscopoNaoAplicavel() {
        UUID outroInsumo = UUID.randomUUID();
        var soOutro = AlertaConfig.novo(TipoAlerta.RUPTURA, outroInsumo, null, null, 0, null, clock.instant());
        var escolhida = avaliador.melhorMatch(List.of(soOutro), insumo, filial);
        assertThat(escolhida).isNull();
    }

    @Test
    void avaliarEstoque_rupturaNovaQuandoSaldoZero_disparaAlerta() {
        var configRup = AlertaConfig.novo(TipoAlerta.RUPTURA, null, null, null, 0, null, clock.instant());
        when(saldoRepo.somarPorInsumoEFilial(insumo, filial)).thenReturn(BigDecimal.ZERO);
        when(configRepo.findAtivasParaEscopo(TipoAlerta.RUPTURA, insumo, filial)).thenReturn(List.of(configRup));
        when(configRepo.findAtivasParaEscopo(TipoAlerta.ESTOQUE_MINIMO_PERCENTUAL, insumo, filial)).thenReturn(List.of());
        when(configRepo.findAtivasParaEscopo(TipoAlerta.ESTOQUE_MINIMO_ABSOLUTO, insumo, filial)).thenReturn(List.of());
        when(disparadoRepo.findAtivoSemLote(eq(configRup.id()), eq(insumo), eq(filial))).thenReturn(Optional.empty());

        avaliador.avaliarEstoque(insumo, filial);

        verify(disparadoRepo).save(any(AlertaDisparado.class));
    }

    @Test
    void avaliarEstoque_rupturaJaAtiva_naoDuplica() {
        var configRup = AlertaConfig.novo(TipoAlerta.RUPTURA, null, null, null, 0, null, clock.instant());
        var jaAtivo = AlertaDisparado.disparar(configRup.id(), TipoAlerta.RUPTURA,
                insumo, filial, null, BigDecimal.ZERO, "x", clock.instant());

        when(saldoRepo.somarPorInsumoEFilial(insumo, filial)).thenReturn(BigDecimal.ZERO);
        when(configRepo.findAtivasParaEscopo(TipoAlerta.RUPTURA, insumo, filial)).thenReturn(List.of(configRup));
        when(configRepo.findAtivasParaEscopo(TipoAlerta.ESTOQUE_MINIMO_PERCENTUAL, insumo, filial)).thenReturn(List.of());
        when(configRepo.findAtivasParaEscopo(TipoAlerta.ESTOQUE_MINIMO_ABSOLUTO, insumo, filial)).thenReturn(List.of());
        when(disparadoRepo.findAtivoSemLote(eq(configRup.id()), eq(insumo), eq(filial))).thenReturn(Optional.of(jaAtivo));

        avaliador.avaliarEstoque(insumo, filial);

        verify(disparadoRepo, never()).save(any());
    }

    @Test
    void avaliarEstoque_saldoNormalizou_resolveAuto() {
        var configRup = AlertaConfig.novo(TipoAlerta.RUPTURA, null, null, null, 0, null, clock.instant());
        var jaAtivo = AlertaDisparado.disparar(configRup.id(), TipoAlerta.RUPTURA,
                insumo, filial, null, BigDecimal.ZERO, "x", clock.instant());

        when(saldoRepo.somarPorInsumoEFilial(insumo, filial)).thenReturn(new BigDecimal("100"));
        when(configRepo.findAtivasParaEscopo(TipoAlerta.RUPTURA, insumo, filial)).thenReturn(List.of(configRup));
        when(configRepo.findAtivasParaEscopo(TipoAlerta.ESTOQUE_MINIMO_PERCENTUAL, insumo, filial)).thenReturn(List.of());
        when(configRepo.findAtivasParaEscopo(TipoAlerta.ESTOQUE_MINIMO_ABSOLUTO, insumo, filial)).thenReturn(List.of());
        when(disparadoRepo.findAtivoSemLote(eq(configRup.id()), eq(insumo), eq(filial))).thenReturn(Optional.of(jaAtivo));

        avaliador.avaliarEstoque(insumo, filial);

        assertThat(jaAtivo.status()).isEqualTo(StatusAlerta.RESOLVIDO_AUTO);
        verify(disparadoRepo).save(jaAtivo);
    }

    @Test
    void percentual_semEstoqueMaximo_naoDispara() {
        var configPct = AlertaConfig.novo(TipoAlerta.ESTOQUE_MINIMO_PERCENTUAL, null, null,
                new BigDecimal("20"), 0, null, clock.instant());
        when(saldoRepo.somarPorInsumoEFilial(insumo, filial)).thenReturn(new BigDecimal("5"));
        when(configRepo.findAtivasParaEscopo(TipoAlerta.RUPTURA, insumo, filial)).thenReturn(List.of());
        when(configRepo.findAtivasParaEscopo(TipoAlerta.ESTOQUE_MINIMO_PERCENTUAL, insumo, filial)).thenReturn(List.of(configPct));
        when(configRepo.findAtivasParaEscopo(TipoAlerta.ESTOQUE_MINIMO_ABSOLUTO, insumo, filial)).thenReturn(List.of());
        when(insumoFilialRepo.findByInsumoEFilial(any(), any())).thenReturn(Optional.empty());
        when(disparadoRepo.findAtivoSemLote(any(), any(), any())).thenReturn(Optional.empty());

        avaliador.avaliarEstoque(insumo, filial);

        verify(disparadoRepo, never()).save(any());
    }
}
