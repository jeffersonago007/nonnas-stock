package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilialTest {

    private static final Instant T0 = Instant.parse("2026-05-08T12:00:00Z");

    @Test
    void novaFilialVemAtiva() {
        Filial f = Filial.nova(EmpresaId.generate(), "Filial Centro", Cnpj.of("11444777000161"),
                "Av. Paulista, 1000", T0);
        assertThat(f.ativa()).isTrue();
        assertThat(f.nome()).isEqualTo("Filial Centro");
        assertThat(f.endereco()).contains("Av. Paulista, 1000");
    }

    @Test
    void enderecoOpcional() {
        Filial f = Filial.nova(EmpresaId.generate(), "Filial Sem Endereco",
                Cnpj.of("11444777000161"), null, T0);
        assertThat(f.endereco()).isEmpty();
    }

    @Test
    void renomearTrimEAtualiza() {
        Filial f = Filial.nova(EmpresaId.generate(), "X", Cnpj.of("11444777000161"), null, T0);
        f.renomear("  Novo Nome  ", T0.plusSeconds(60));
        assertThat(f.nome()).isEqualTo("Novo Nome");
    }

    @Test
    void atualizarEndereco() {
        Filial f = Filial.nova(EmpresaId.generate(), "X", Cnpj.of("11444777000161"), null, T0);
        f.atualizarEndereco("Rua A, 1", T0.plusSeconds(60));
        assertThat(f.endereco()).contains("Rua A, 1");
    }

    @Test
    void desativarEAtivar() {
        Filial f = Filial.nova(EmpresaId.generate(), "X", Cnpj.of("11444777000161"), null, T0);
        f.desativar(T0);
        assertThat(f.ativa()).isFalse();
        f.ativar(T0);
        assertThat(f.ativa()).isTrue();
    }

    @Test
    void rejeitaNomeVazio() {
        assertThatThrownBy(() -> Filial.nova(EmpresaId.generate(), "  ",
                Cnpj.of("11444777000161"), null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaNomeAcimaDe255() {
        String big = "x".repeat(256);
        assertThatThrownBy(() -> Filial.nova(EmpresaId.generate(), big,
                Cnpj.of("11444777000161"), null, T0))
                .isInstanceOf(ValidationException.class);
    }
}
