package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CnpjTest {

    // Real-world example used by the SEFAZ for testing.
    private static final String VALID_CNPJ_RAW = "11444777000161";
    private static final String VALID_CNPJ_FORMATTED = "11.444.777/0001-61";

    @Test
    void aceitaCnpjValido() {
        Cnpj c = Cnpj.of(VALID_CNPJ_RAW);
        assertThat(c.value()).isEqualTo(VALID_CNPJ_RAW);
    }

    @Test
    void normalizaTirandoMascara() {
        Cnpj c = Cnpj.of(VALID_CNPJ_FORMATTED);
        assertThat(c.value()).isEqualTo(VALID_CNPJ_RAW);
    }

    @Test
    void formattedAplicaMascara() {
        assertThat(Cnpj.of(VALID_CNPJ_RAW).formatted()).isEqualTo(VALID_CNPJ_FORMATTED);
    }

    @Test
    void rejeitaTamanhoErrado() {
        assertThatThrownBy(() -> Cnpj.of("123"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaTodosDigitosIguais() {
        assertThatThrownBy(() -> Cnpj.of("11111111111111"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaDigitoVerificadorErrado() {
        // Same as valid but with last digit changed.
        assertThatThrownBy(() -> Cnpj.of("11444777000162"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaSegundoDigitoVerificadorErrado() {
        assertThatThrownBy(() -> Cnpj.of("11444777000171"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaNull() {
        assertThatThrownBy(() -> Cnpj.of(null)).isInstanceOf(NullPointerException.class);
    }
}
