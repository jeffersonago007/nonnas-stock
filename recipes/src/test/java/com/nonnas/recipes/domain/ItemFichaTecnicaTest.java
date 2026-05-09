package com.nonnas.recipes.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemFichaTecnicaTest {

    @Test
    void novo_aceitaCamposValidos() {
        UUID insumo = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();
        var item = ItemFichaTecnica.novo(insumo, unidade, new BigDecimal("0.5"));

        assertThat(item.id()).isNotNull();
        assertThat(item.insumoId()).isEqualTo(insumo);
        assertThat(item.unidadeId()).isEqualTo(unidade);
        assertThat(item.quantidade()).isEqualByComparingTo("0.5");
    }

    @Test
    void quantidadeZero_lancaValidacao() {
        assertThatThrownBy(() -> ItemFichaTecnica.novo(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("positiva");
    }

    @Test
    void quantidadeNegativa_lancaValidacao() {
        assertThatThrownBy(() -> ItemFichaTecnica.novo(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("-1")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void insumoNulo_lancaNullPointer() {
        assertThatThrownBy(() -> ItemFichaTecnica.novo(null, UUID.randomUUID(), BigDecimal.ONE))
                .isInstanceOf(NullPointerException.class);
    }
}
