package com.nonnas.identity.application.password;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SenhaValidaValidatorTest {

    private final SenhaValidaValidator v = new SenhaValidaValidator();

    @Test
    void aceitaSenhaConforme() {
        assertThat(v.isValid("AdminNonnas2026!", null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Short1!",                  // < 10 chars
            "abcdefghij",               // sem dígito, sem especial
            "abcdefghi1",               // sem especial
            "abcdefghi!",               // sem dígito
            "1234567890!",              // sem letra
            "!!!!!!!!!!"                // só especiais
    })
    void rejeitaSenhasFracas(String senha) {
        assertThat(v.isValid(senha, null)).isFalse();
    }

    @Test
    void rejeitaNull() {
        assertThat(v.isValid(null, null)).isFalse();
    }

    @Test
    void rejeitaSenhaApenasComEspacosEEspeciais() {
        assertThat(v.isValid("          ", null)).isFalse();
    }
}
