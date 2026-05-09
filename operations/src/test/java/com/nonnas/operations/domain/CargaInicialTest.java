package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CargaInicialTest {

    private final String hashValido = "0".repeat(64);
    private final Instant t0 = Instant.parse("2026-05-08T10:00:00Z");

    @Test
    void novo_camposValidos() {
        var c = CargaInicial.novo(UUID.randomUUID(), hashValido, "carga.xlsx",
                10, 0, UUID.randomUUID(), t0);
        assertThat(c.id()).isNotNull();
        assertThat(c.hashPlanilha()).isEqualTo(hashValido);
        assertThat(c.registrosProcessados()).isEqualTo(10);
        assertThat(c.registrosFalhos()).isEqualTo(0);
    }

    @Test
    void hashTamanhoErrado_lancaValidacao() {
        assertThatThrownBy(() -> CargaInicial.novo(UUID.randomUUID(), "abc",
                "x.xlsx", 1, 0, UUID.randomUUID(), t0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("64");
    }

    @Test
    void contadoresNegativos_lancaValidacao() {
        assertThatThrownBy(() -> CargaInicial.novo(UUID.randomUUID(), hashValido,
                "x.xlsx", -1, 0, UUID.randomUUID(), t0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void nomeArquivoVazio_lancaValidacao() {
        assertThatThrownBy(() -> CargaInicial.novo(UUID.randomUUID(), hashValido,
                "  ", 1, 0, UUID.randomUUID(), t0))
                .isInstanceOf(ValidationException.class);
    }
}
