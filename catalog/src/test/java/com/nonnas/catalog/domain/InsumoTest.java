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
    void renomearAtualizaENormalizaUppercase() {
        Insumo i = novoInsumo();
        i.renomear("Mussarela Light", T0);
        assertThat(i.nome()).isEqualTo("MUSSARELA LIGHT");
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

    @Test
    void novoVemSemDiasAlertaVencimento() {
        Insumo i = novoInsumo();
        assertThat(i.diasAlertaVencimento()).isEmpty();
    }

    @Test
    void definirDiasAlertaVencimentoAceitaIntervalo() {
        Insumo i = novoInsumo();
        i.definirDiasAlertaVencimento(7, T0);
        assertThat(i.diasAlertaVencimento()).contains(7);
        i.definirDiasAlertaVencimento(90, T0);
        assertThat(i.diasAlertaVencimento()).contains(90);
        i.definirDiasAlertaVencimento(1, T0);
        assertThat(i.diasAlertaVencimento()).contains(1);
        i.definirDiasAlertaVencimento(null, T0);
        assertThat(i.diasAlertaVencimento()).isEmpty();
    }

    @Test
    void definirDiasAlertaVencimentoRejeitaForaDoIntervalo() {
        Insumo i = novoInsumo();
        assertThatThrownBy(() -> i.definirDiasAlertaVencimento(0, T0))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> i.definirDiasAlertaVencimento(91, T0))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> i.definirDiasAlertaVencimento(-1, T0))
                .isInstanceOf(ValidationException.class);
    }
}
