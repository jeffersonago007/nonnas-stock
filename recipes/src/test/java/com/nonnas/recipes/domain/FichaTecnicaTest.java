package com.nonnas.recipes.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FichaTecnicaTest {

    private final ProdutoVendavelId produtoId = ProdutoVendavelId.generate();
    private final Instant agora = Instant.parse("2026-05-08T20:00:00Z");

    @Test
    void nova_v1AtivaSemVigenteAte() {
        var ficha = FichaTecnica.nova(produtoId, 1, List.of(itemNovo()), agora);

        assertThat(ficha.versao()).isEqualTo(1);
        assertThat(ficha.ativa()).isTrue();
        assertThat(ficha.vigenteDesde()).isEqualTo(agora);
        assertThat(ficha.vigenteAteOpt()).isEmpty();
        assertThat(ficha.itens()).hasSize(1);
    }

    @Test
    void nova_semItens_lancaValidacao() {
        assertThatThrownBy(() -> FichaTecnica.nova(produtoId, 1, List.of(), agora))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ao menos um item");
    }

    @Test
    void nova_insumoDuplicado_lancaValidacao() {
        UUID insumoX = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();
        var i1 = ItemFichaTecnica.novo(insumoX, unidade, new BigDecimal("1"));
        var i2 = ItemFichaTecnica.novo(insumoX, unidade, new BigDecimal("2"));

        assertThatThrownBy(() -> FichaTecnica.nova(produtoId, 1, List.of(i1, i2), agora))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("duplicado");
    }

    @Test
    void nova_versaoZero_lancaValidacao() {
        assertThatThrownBy(() -> FichaTecnica.nova(produtoId, 0, List.of(itemNovo()), agora))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Versão");
    }

    @Test
    void editar_desativaAtualECriaProximaVersao() {
        var v1 = FichaTecnica.nova(produtoId, 1, List.of(itemNovo()), agora);
        Instant depois = agora.plusSeconds(3600);

        var v2 = v1.editar(List.of(itemNovo(), itemNovo()), depois);

        assertThat(v1.ativa()).isFalse();
        assertThat(v1.vigenteAteOpt()).contains(depois);

        assertThat(v2.versao()).isEqualTo(2);
        assertThat(v2.ativa()).isTrue();
        assertThat(v2.vigenteDesde()).isEqualTo(depois);
        assertThat(v2.vigenteAteOpt()).isEmpty();
        assertThat(v2.itens()).hasSize(2);
        assertThat(v2.produtoVendavelId()).isEqualTo(produtoId);
    }

    @Test
    void editar_fichaInativa_lancaValidacao() {
        var v1 = FichaTecnica.nova(produtoId, 1, List.of(itemNovo()), agora);
        v1.editar(List.of(itemNovo()), agora.plusSeconds(60));  // primeira edição: v1 fica inativa

        assertThatThrownBy(() -> v1.editar(List.of(itemNovo()), agora.plusSeconds(120)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("inativa");
    }

    @Test
    void itensRetornados_imutaveis() {
        var ficha = FichaTecnica.nova(produtoId, 1, List.of(itemNovo()), agora);
        var itens = ficha.itens();

        assertThatThrownBy(() -> itens.add(itemNovo()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private ItemFichaTecnica itemNovo() {
        return ItemFichaTecnica.novo(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("0.25"));
    }
}
