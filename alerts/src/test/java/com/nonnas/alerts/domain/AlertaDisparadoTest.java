package com.nonnas.alerts.domain;

import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertaDisparadoTest {

    private final Instant t0 = Instant.parse("2026-05-08T10:00:00Z");

    private AlertaDisparado disparoNovo() {
        return AlertaDisparado.disparar(AlertaConfigId.generate(), TipoAlerta.RUPTURA,
                UUID.randomUUID(), UUID.randomUUID(), null,
                BigDecimal.ZERO, "saldo zerado", t0);
    }

    @Test
    void disparar_iniciaAtivo() {
        var a = disparoNovo();
        assertThat(a.status()).isEqualTo(StatusAlerta.ATIVO);
        assertThat(a.dataDisparo()).isEqualTo(t0);
        assertThat(a.dataResolucaoOpt()).isEmpty();
    }

    @Test
    void resolverAuto_ativo_marcaResolvido() {
        var a = disparoNovo();
        a.resolverAuto(t0.plusSeconds(3600));
        assertThat(a.status()).isEqualTo(StatusAlerta.RESOLVIDO_AUTO);
        assertThat(a.dataResolucaoOpt()).contains(t0.plusSeconds(3600));
    }

    @Test
    void resolverAutoOuManual_seJaResolvido_lancaErroBR() {
        var a = disparoNovo();
        a.resolverAuto(t0.plusSeconds(60));
        assertThatThrownBy(() -> a.resolverAuto(t0.plusSeconds(120)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> a.resolverManual(UUID.randomUUID(), t0.plusSeconds(120)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void resolverManual_anotaResolvidoPor() {
        var a = disparoNovo();
        UUID gerente = UUID.randomUUID();
        a.resolverManual(gerente, t0.plusSeconds(120));

        assertThat(a.status()).isEqualTo(StatusAlerta.RESOLVIDO_MANUAL);
        assertThat(a.resolvidoPorOpt()).contains(gerente);
    }

    @Test
    void marcarVisualizado_ortogonalAoStatus() {
        var a = disparoNovo();
        UUID usuario = UUID.randomUUID();
        a.marcarVisualizado(usuario, t0.plusSeconds(60));

        assertThat(a.visualizadoEmOpt()).isPresent();
        assertThat(a.visualizadoPorOpt()).contains(usuario);
        assertThat(a.status()).isEqualTo(StatusAlerta.ATIVO);  // status não muda

        // Pode resolver depois
        a.resolverAuto(t0.plusSeconds(120));
        assertThat(a.status()).isEqualTo(StatusAlerta.RESOLVIDO_AUTO);
        // visualização permanece
        assertThat(a.visualizadoEmOpt()).isPresent();
    }

    @Test
    void marcarVisualizado_duasVezes_lancaValidacao() {
        var a = disparoNovo();
        a.marcarVisualizado(UUID.randomUUID(), t0);
        assertThatThrownBy(() -> a.marcarVisualizado(UUID.randomUUID(), t0.plusSeconds(60)))
                .isInstanceOf(ValidationException.class);
    }
}
