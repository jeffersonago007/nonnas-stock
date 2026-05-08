package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoriaInsumoTest {

    private static final Instant T0 = Instant.parse("2026-05-08T12:00:00Z");

    @Test
    void novaSemPaiVemAtivaTrim() {
        CategoriaInsumo c = CategoriaInsumo.nova("  Laticínios  ", null, T0);
        assertThat(c.nome()).isEqualTo("Laticínios");
        assertThat(c.ativa()).isTrue();
        assertThat(c.categoriaPaiId()).isEmpty();
    }

    @Test
    void novaComPai() {
        CategoriaInsumoId pai = CategoriaInsumoId.generate();
        CategoriaInsumo c = CategoriaInsumo.nova("Mussarela", pai, T0);
        assertThat(c.categoriaPaiId()).contains(pai);
    }

    @Test
    void renomearAtualiza() {
        CategoriaInsumo c = CategoriaInsumo.nova("X", null, T0);
        c.renomear("Y", T0);
        assertThat(c.nome()).isEqualTo("Y");
    }

    @Test
    void desativarEAtivar() {
        CategoriaInsumo c = CategoriaInsumo.nova("X", null, T0);
        c.desativar(T0);
        assertThat(c.ativa()).isFalse();
        c.ativar(T0);
        assertThat(c.ativa()).isTrue();
    }

    @Test
    void rejeitaNomeVazio() {
        assertThatThrownBy(() -> CategoriaInsumo.nova("  ", null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaNomeLongo() {
        assertThatThrownBy(() -> CategoriaInsumo.nova("x".repeat(256), null, T0))
                .isInstanceOf(ValidationException.class);
    }
}
