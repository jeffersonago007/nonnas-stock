package com.nonnas.recipes.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdutoVendavelTest {

    private final Instant agora = Instant.parse("2026-05-08T20:00:00Z");

    @Test
    void novo_aceitaCamposValidos() {
        var p = ProdutoVendavel.novo("PIZ-001", "Pizza Margherita", "Pizza", agora);

        assertThat(p.codigo()).isEqualTo("PIZ-001");
        assertThat(p.nome()).isEqualTo("Pizza Margherita");
        assertThat(p.categoria()).isEqualTo("Pizza");
        assertThat(p.ativo()).isTrue();
        assertThat(p.createdAt()).isEqualTo(agora);
        assertThat(p.updatedAt()).isEqualTo(agora);
    }

    @Test
    void novo_codigoEmBranco_lancaValidacao() {
        assertThatThrownBy(() -> ProdutoVendavel.novo("  ", "Pizza", "Pizza", agora))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Código");
    }

    @Test
    void novo_nomeMuitoGrande_lancaValidacao() {
        String nomeEnorme = "x".repeat(151);
        assertThatThrownBy(() -> ProdutoVendavel.novo("PIZ-001", nomeEnorme, "Pizza", agora))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Nome");
    }

    @Test
    void novo_categoriaNula_lancaValidacao() {
        assertThatThrownBy(() -> ProdutoVendavel.novo("PIZ-001", "Pizza", null, agora))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Categoria");
    }

    @Test
    void renomear_atualizaUpdatedAt() {
        var p = ProdutoVendavel.novo("PIZ-001", "Pizza", "Pizza", agora);
        Instant depois = agora.plusSeconds(60);

        p.renomear("Pizza Margherita", depois);

        assertThat(p.nome()).isEqualTo("Pizza Margherita");
        assertThat(p.updatedAt()).isEqualTo(depois);
    }

    @Test
    void recategorizar_atualizaCategoria() {
        var p = ProdutoVendavel.novo("PIZ-001", "Pizza", "Pizza", agora);
        Instant depois = agora.plusSeconds(60);

        p.recategorizar("Pizza Especial", depois);

        assertThat(p.categoria()).isEqualTo("Pizza Especial");
        assertThat(p.updatedAt()).isEqualTo(depois);
    }

    @Test
    void desativarEAtivar_alternaFlag() {
        var p = ProdutoVendavel.novo("PIZ-001", "Pizza", "Pizza", agora);
        Instant depois = agora.plusSeconds(60);

        p.desativar(depois);
        assertThat(p.ativo()).isFalse();

        p.ativar(depois);
        assertThat(p.ativo()).isTrue();
    }
}
