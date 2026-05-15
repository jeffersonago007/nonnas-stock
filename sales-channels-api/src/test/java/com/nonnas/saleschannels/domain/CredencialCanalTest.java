package com.nonnas.saleschannels.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredencialCanalTest {

    private static final Instant T0 = Instant.parse("2026-05-15T18:00:00Z");
    private static final UUID FILIAL = UUID.randomUUID();

    private CredencialCanal cred() {
        return CredencialCanal.nova(
                CanalTipo.IFOOD, FILIAL,
                "merchant-abc", "client-xyz", "AES256GCM:cifrado",
                "https://merchant-api.ifood.com.br", "loja teste", T0);
    }

    @Test
    void nasceAtivaComCamposObrigatorios() {
        CredencialCanal c = cred();
        assertThat(c.ativa()).isTrue();
        assertThat(c.canalTipo()).isEqualTo(CanalTipo.IFOOD);
        assertThat(c.merchantExternoId()).isEqualTo("merchant-abc");
        assertThat(c.clientSecretCifrado()).isEqualTo("AES256GCM:cifrado");
        assertThat(c.baseUrlOpt()).contains("https://merchant-api.ifood.com.br");
        assertThat(c.createdAt()).isEqualTo(T0);
        assertThat(c.updatedAt()).isEqualTo(T0);
    }

    @Test
    void rotacionarSegredoAtualizaTimestamp() {
        CredencialCanal c = cred();
        Instant t1 = T0.plusSeconds(3600);
        c.rotacionarSegredo("AES256GCM:novo", t1);
        assertThat(c.clientSecretCifrado()).isEqualTo("AES256GCM:novo");
        assertThat(c.updatedAt()).isEqualTo(t1);
    }

    @Test
    void ativarEDesativarFlipam() {
        CredencialCanal c = cred();
        Instant t1 = T0.plusSeconds(60);
        c.desativar(t1);
        assertThat(c.ativa()).isFalse();
        assertThat(c.updatedAt()).isEqualTo(t1);

        Instant t2 = T0.plusSeconds(120);
        c.ativar(t2);
        assertThat(c.ativa()).isTrue();
        assertThat(c.updatedAt()).isEqualTo(t2);
    }

    @Test
    void rejeitaCamposObrigatoriosVazios() {
        assertThatThrownBy(() -> CredencialCanal.nova(
                CanalTipo.IFOOD, FILIAL, "", "client", "secret", null, null, T0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("merchantExternoId");

        assertThatThrownBy(() -> CredencialCanal.nova(
                CanalTipo.IFOOD, FILIAL, "merchant", "", "secret", null, null, T0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("clientId");

        assertThatThrownBy(() -> CredencialCanal.nova(
                CanalTipo.IFOOD, FILIAL, "merchant", "client", "  ", null, null, T0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("clientSecretCifrado");
    }
}
