package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnidadeMedidaTest {

    private static final Instant T0 = Instant.parse("2026-05-08T12:00:00Z");

    @Test
    void novaNormalizaCodigoParaUppercase() {
        UnidadeMedida u = UnidadeMedida.nova("kg", "Quilograma", UnidadeMedidaTipo.PESO, T0);
        assertThat(u.codigo()).isEqualTo("KG");
        assertThat(u.ativa()).isTrue();
    }

    @Test
    void desativarEAtivar() {
        UnidadeMedida u = UnidadeMedida.nova("KG", "Quilograma", UnidadeMedidaTipo.PESO, T0);
        u.desativar(T0);
        assertThat(u.ativa()).isFalse();
        u.ativar(T0);
        assertThat(u.ativa()).isTrue();
    }

    @Test
    void rejeitaCodigoVazio() {
        assertThatThrownBy(() -> UnidadeMedida.nova("  ", "x", UnidadeMedidaTipo.PESO, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaNomeVazio() {
        assertThatThrownBy(() -> UnidadeMedida.nova("KG", "  ", UnidadeMedidaTipo.PESO, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaCodigoLongo() {
        assertThatThrownBy(() -> UnidadeMedida.nova("X".repeat(21), "Nome", UnidadeMedidaTipo.PESO, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaNomeLongo() {
        assertThatThrownBy(() -> UnidadeMedida.nova("X", "X".repeat(101), UnidadeMedidaTipo.PESO, T0))
                .isInstanceOf(ValidationException.class);
    }
}
