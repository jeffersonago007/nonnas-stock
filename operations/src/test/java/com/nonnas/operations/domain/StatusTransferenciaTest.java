package com.nonnas.operations.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatusTransferenciaTest {

    @Test
    void podeAprovar_apenasSolicitada() {
        assertThat(StatusTransferencia.SOLICITADA.podeAprovar()).isTrue();
        assertThat(StatusTransferencia.APROVADA.podeAprovar()).isFalse();
        assertThat(StatusTransferencia.EM_TRANSITO.podeAprovar()).isFalse();
        assertThat(StatusTransferencia.RECEBIDA.podeAprovar()).isFalse();
        assertThat(StatusTransferencia.CANCELADA.podeAprovar()).isFalse();
    }

    @Test
    void podeEnviar_apenasAprovada() {
        assertThat(StatusTransferencia.APROVADA.podeEnviar()).isTrue();
        for (var s : StatusTransferencia.values()) {
            if (s != StatusTransferencia.APROVADA) {
                assertThat(s.podeEnviar()).isFalse();
            }
        }
    }

    @Test
    void podeReceber_apenasEmTransito() {
        assertThat(StatusTransferencia.EM_TRANSITO.podeReceber()).isTrue();
        for (var s : StatusTransferencia.values()) {
            if (s != StatusTransferencia.EM_TRANSITO) {
                assertThat(s.podeReceber()).isFalse();
            }
        }
    }

    @Test
    void podeCancelar_apenasSolicitadaOuAprovada() {
        assertThat(StatusTransferencia.SOLICITADA.podeCancelar()).isTrue();
        assertThat(StatusTransferencia.APROVADA.podeCancelar()).isTrue();
        assertThat(StatusTransferencia.EM_TRANSITO.podeCancelar()).isFalse();
        assertThat(StatusTransferencia.RECEBIDA.podeCancelar()).isFalse();
        assertThat(StatusTransferencia.CANCELADA.podeCancelar()).isFalse();
    }

    @Test
    void isFinal_apenasRecebidaOuCancelada() {
        assertThat(StatusTransferencia.RECEBIDA.isFinal()).isTrue();
        assertThat(StatusTransferencia.CANCELADA.isFinal()).isTrue();
        assertThat(StatusTransferencia.SOLICITADA.isFinal()).isFalse();
        assertThat(StatusTransferencia.APROVADA.isFinal()).isFalse();
        assertThat(StatusTransferencia.EM_TRANSITO.isFinal()).isFalse();
    }
}
