package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RazaoSocialTest {

    @Test
    void aceitaValido() {
        assertThat(RazaoSocial.of("Nonnas Paola Ltda").value()).isEqualTo("Nonnas Paola Ltda");
    }

    @Test
    void faceTrim() {
        assertThat(RazaoSocial.of("  Nonnas Paola  ").value()).isEqualTo("Nonnas Paola");
    }

    @Test
    void rejeitaVazio() {
        assertThatThrownBy(() -> RazaoSocial.of("   ")).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaAcimaDe255Chars() {
        String big = "x".repeat(256);
        assertThatThrownBy(() -> RazaoSocial.of(big)).isInstanceOf(ValidationException.class);
    }
}
