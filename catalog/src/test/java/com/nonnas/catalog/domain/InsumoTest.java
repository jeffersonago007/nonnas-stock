package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsumoTest {

    private static final Instant T0 = Instant.parse("2026-05-08T12:00:00Z");

    private Insumo novoInsumo() {
        return Insumo.novo("INS-001", "Mussarela",
                CategoriaInsumoId.generate(), UnidadeMedidaId.generate(), true, true, T0);
    }

    @Test
    void novoVemAtivoEControlaLote() {
        Insumo i = novoInsumo();
        assertThat(i.ativo()).isTrue();
        assertThat(i.controlaLote()).isTrue();
        assertThat(i.controlaValidade()).isTrue();
    }

    @Test
    void renomearAtualiza() {
        Insumo i = novoInsumo();
        i.renomear("Mussarela Light", T0);
        assertThat(i.nome()).isEqualTo("Mussarela Light");
    }

    @Test
    void desativarEAtivar() {
        Insumo i = novoInsumo();
        i.desativar(T0);
        assertThat(i.ativo()).isFalse();
        i.ativar(T0);
        assertThat(i.ativo()).isTrue();
    }

    @Test
    void rejeitaCodigoVazio() {
        assertThatThrownBy(() -> Insumo.novo("  ", "Nome",
                CategoriaInsumoId.generate(), UnidadeMedidaId.generate(), true, true, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaNomeVazio() {
        assertThatThrownBy(() -> Insumo.novo("INS", "  ",
                CategoriaInsumoId.generate(), UnidadeMedidaId.generate(), true, true, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaCodigoLongo() {
        assertThatThrownBy(() -> Insumo.novo("X".repeat(51), "Nome",
                CategoriaInsumoId.generate(), UnidadeMedidaId.generate(), true, true, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaNomeLongo() {
        assertThatThrownBy(() -> Insumo.novo("INS", "X".repeat(256),
                CategoriaInsumoId.generate(), UnidadeMedidaId.generate(), true, true, T0))
                .isInstanceOf(ValidationException.class);
    }
}
