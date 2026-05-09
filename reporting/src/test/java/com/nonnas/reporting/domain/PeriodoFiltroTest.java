package com.nonnas.reporting.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodoFiltroTest {

    @Test
    void aceita_periodoValido() {
        Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
        Instant fim = Instant.parse("2026-01-31T23:59:59Z");

        var p = new PeriodoFiltro(inicio, fim);

        assertThat(p.inicio()).isEqualTo(inicio);
        assertThat(p.fim()).isEqualTo(fim);
    }

    @Test
    void rejeita_inicioNulo() {
        assertThatThrownBy(() -> new PeriodoFiltro(null, Instant.now()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("obrigatórios");
    }

    @Test
    void rejeita_fimNulo() {
        assertThatThrownBy(() -> new PeriodoFiltro(Instant.now(), null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("obrigatórios");
    }

    @Test
    void rejeita_fimAnteriorAoInicio() {
        Instant inicio = Instant.parse("2026-01-31T00:00:00Z");
        Instant fim = Instant.parse("2026-01-01T00:00:00Z");

        assertThatThrownBy(() -> new PeriodoFiltro(inicio, fim))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("anterior");
    }
}
