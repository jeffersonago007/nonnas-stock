package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SenhaHashTest {

    private static final String VALID_2A = "$2a$12$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRS";
    private static final String VALID_2B = "$2b$12$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRS";

    @Test
    void aceitaPrefixo2a() {
        assertThat(SenhaHash.of(VALID_2A).value()).isEqualTo(VALID_2A);
    }

    @Test
    void aceitaPrefixo2b() {
        assertThat(SenhaHash.of(VALID_2B).value()).startsWith("$2b$");
    }

    @Test
    void rejeitaPrefixoInvalido() {
        assertThatThrownBy(() -> SenhaHash.of("md5:abc")).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaNull() {
        assertThatThrownBy(() -> SenhaHash.of(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void toStringMascaraValor() {
        assertThat(SenhaHash.of(VALID_2A).toString()).isEqualTo("SenhaHash[***]");
    }
}
