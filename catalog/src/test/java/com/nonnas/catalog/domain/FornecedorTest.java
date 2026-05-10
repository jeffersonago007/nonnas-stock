package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FornecedorTest {

    private static final Instant T0 = Instant.parse("2026-05-08T12:00:00Z");

    @Test
    void novoVemAtivoComRazaoNormalizadaUppercase() {
        Fornecedor f = Fornecedor.novo("Atacado SP Ltda", Cnpj.of("11444777000161"), T0);
        assertThat(f.ativo()).isTrue();
        assertThat(f.razaoSocial()).isEqualTo("ATACADO SP LTDA");
    }

    @Test
    void renomearTambemNormalizaUppercase() {
        Fornecedor f = Fornecedor.novo("Atacado SP", Cnpj.of("11444777000161"), T0);
        f.renomear("Atacado SP Ltda", T0);
        assertThat(f.razaoSocial()).isEqualTo("ATACADO SP LTDA");
    }

    @Test
    void desativarEAtivar() {
        Fornecedor f = Fornecedor.novo("X", Cnpj.of("11444777000161"), T0);
        f.desativar(T0);
        assertThat(f.ativo()).isFalse();
        f.ativar(T0);
        assertThat(f.ativo()).isTrue();
    }

    @Test
    void rejeitaRazaoVazia() {
        assertThatThrownBy(() -> Fornecedor.novo("  ", Cnpj.of("11444777000161"), T0))
                .isInstanceOf(ValidationException.class);
    }
}
