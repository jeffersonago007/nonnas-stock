package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CnpjTest {

    @Test
    void aceitaCnpjValido() {
        assertThat(Cnpj.of("11444777000161").value()).isEqualTo("11444777000161");
    }

    @Test
    void normalizaTirandoMascara() {
        assertThat(Cnpj.of("11.444.777/0001-61").value()).isEqualTo("11444777000161");
    }

    @Test
    void formattedAplicaMascara() {
        assertThat(Cnpj.of("11444777000161").formatted()).isEqualTo("11.444.777/0001-61");
    }

    @Test
    void rejeitaTodosDigitosIguais() {
        assertThatThrownBy(() -> Cnpj.of("11111111111111"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaTamanhoErrado() {
        assertThatThrownBy(() -> Cnpj.of("123")).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaDigitoVerificadorErrado() {
        assertThatThrownBy(() -> Cnpj.of("11444777000162")).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaNull() {
        assertThatThrownBy(() -> Cnpj.of(null)).isInstanceOf(NullPointerException.class);
    }
}
