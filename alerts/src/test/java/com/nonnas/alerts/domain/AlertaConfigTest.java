package com.nonnas.alerts.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertaConfigTest {

    private final Instant t0 = Instant.parse("2026-05-08T10:00:00Z");

    @Test
    void rupturaSemThreshold_ok() {
        var c = AlertaConfig.novo(TipoAlerta.RUPTURA, null, null, null, 0, null, t0);
        assertThat(c.tipo()).isEqualTo(TipoAlerta.RUPTURA);
        assertThat(c.thresholdOpt()).isEmpty();
        assertThat(c.especificidade()).isZero();
    }

    @Test
    void rupturaComThreshold_lancaValidacao() {
        assertThatThrownBy(() -> AlertaConfig.novo(TipoAlerta.RUPTURA, null, null,
                BigDecimal.TEN, 0, null, t0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("RUPTURA");
    }

    @Test
    void percentualForaDoIntervalo_lancaValidacao() {
        assertThatThrownBy(() -> AlertaConfig.novo(TipoAlerta.ESTOQUE_MINIMO_PERCENTUAL, null, null,
                new BigDecimal("150"), 0, null, t0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("100");
    }

    @Test
    void absolutoZero_lancaValidacao() {
        assertThatThrownBy(() -> AlertaConfig.novo(TipoAlerta.ESTOQUE_MINIMO_ABSOLUTO, null, null,
                BigDecimal.ZERO, 0, null, t0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void diasNaoInteiro_lancaValidacao() {
        assertThatThrownBy(() -> AlertaConfig.novo(TipoAlerta.VENCIMENTO_PROXIMO_DIAS, null, null,
                new BigDecimal("3.5"), 0, null, t0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("inteiro");
    }

    @Test
    void especificidade_aumentaConformeFiltrosNaoNulos() {
        var todos = AlertaConfig.novo(TipoAlerta.RUPTURA, null, null, null, 0, null, t0);
        var soInsumo = AlertaConfig.novo(TipoAlerta.RUPTURA, UUID.randomUUID(), null, null, 0, null, t0);
        var soFilial = AlertaConfig.novo(TipoAlerta.RUPTURA, null, UUID.randomUUID(), null, 0, null, t0);
        var ambos = AlertaConfig.novo(TipoAlerta.RUPTURA, UUID.randomUUID(), UUID.randomUUID(), null, 0, null, t0);

        assertThat(todos.especificidade()).isEqualTo(0);
        assertThat(soFilial.especificidade()).isEqualTo(1);
        assertThat(soInsumo.especificidade()).isEqualTo(2);
        assertThat(ambos.especificidade()).isEqualTo(3);
    }

    @Test
    void atualizar_validaTipoNovamente() {
        var c = AlertaConfig.novo(TipoAlerta.ESTOQUE_MINIMO_PERCENTUAL, null, null,
                new BigDecimal("20"), 0, null, t0);
        c.atualizar(new BigDecimal("30"), 5, "novo motivo", t0.plusSeconds(60));
        assertThat(c.thresholdOpt()).contains(new BigDecimal("30"));
        assertThat(c.prioridade()).isEqualTo(5);
        assertThat(c.observacaoOpt()).contains("novo motivo");

        // Threshold inválido na atualização lança
        assertThatThrownBy(() -> c.atualizar(new BigDecimal("200"), 0, null, t0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void desativar_eAtivar_alternaFlag() {
        var c = AlertaConfig.novo(TipoAlerta.RUPTURA, null, null, null, 0, null, t0);
        c.desativar(t0.plusSeconds(60));
        assertThat(c.ativo()).isFalse();
        c.ativar(t0.plusSeconds(120));
        assertThat(c.ativo()).isTrue();
    }
}
