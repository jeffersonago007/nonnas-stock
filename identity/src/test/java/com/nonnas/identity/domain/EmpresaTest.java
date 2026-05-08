package com.nonnas.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmpresaTest {

    private static final Instant T0 = Instant.parse("2026-05-08T12:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);

    @Test
    void novaEmpresaVemAtiva() {
        Empresa e = Empresa.nova(RazaoSocial.of("Acme"), Cnpj.of("11444777000161"), T0);
        assertThat(e.ativa()).isTrue();
        assertThat(e.razaoSocial().value()).isEqualTo("Acme");
        assertThat(e.createdAt()).isEqualTo(T0);
        assertThat(e.updatedAt()).isEqualTo(T0);
    }

    @Test
    void renomearAtualizaUpdatedAt() {
        Empresa e = Empresa.nova(RazaoSocial.of("Acme"), Cnpj.of("11444777000161"), T0);
        e.renomear(RazaoSocial.of("Acme Reformada"), T1);
        assertThat(e.razaoSocial().value()).isEqualTo("Acme Reformada");
        assertThat(e.updatedAt()).isEqualTo(T1);
    }

    @Test
    void desativarEAtivar() {
        Empresa e = Empresa.nova(RazaoSocial.of("Acme"), Cnpj.of("11444777000161"), T0);
        e.desativar(T1);
        assertThat(e.ativa()).isFalse();
        e.ativar(T1);
        assertThat(e.ativa()).isTrue();
    }

    @Test
    void rejeitaArgumentosNulos() {
        assertThatThrownBy(() ->
                new Empresa(null, RazaoSocial.of("x"), Cnpj.of("11444777000161"), true, T0, T0))
                .isInstanceOf(NullPointerException.class);
    }
}
