package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void aceitaEmailValido() {
        assertThat(Email.of("admin@nonnas.com").value()).isEqualTo("admin@nonnas.com");
    }

    @Test
    void normalizaParaLowercaseETrim() {
        assertThat(Email.of("  AdMin@Nonnas.Com  ").value()).isEqualTo("admin@nonnas.com");
    }

    @Test
    void rejeitaSemArroba() {
        assertThatThrownBy(() -> Email.of("invalid")).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaSemTld() {
        assertThatThrownBy(() -> Email.of("foo@bar")).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaEspacosInternos() {
        assertThatThrownBy(() -> Email.of("a b@c.com")).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaNull() {
        assertThatThrownBy(() -> Email.of(null)).isInstanceOf(NullPointerException.class);
    }
}
