package com.nonnas.reporting.application;

import com.nonnas.reporting.application.ports.RelatorioQueries;
import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VencimentoProximoUseCaseTest {

    @Test
    void aplica_janelaPadraoDe30DiasQuandoNulo() {
        RelatorioQueries queries = mock(RelatorioQueries.class);
        when(queries.vencimentoProximo(any(), anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        new VencimentoProximoUseCase(queries).execute(UUID.randomUUID(), null, 0, 100);

        ArgumentCaptor<Integer> dias = ArgumentCaptor.forClass(Integer.class);
        verify(queries).vencimentoProximo(any(), dias.capture(), eq(0), eq(100));
        assertThat(dias.getValue()).isEqualTo(30);
    }

    @Test
    void aceita_janelaCustomizada() {
        RelatorioQueries queries = mock(RelatorioQueries.class);
        when(queries.vencimentoProximo(any(), anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        new VencimentoProximoUseCase(queries).execute(UUID.randomUUID(), 7, 0, 100);

        verify(queries).vencimentoProximo(any(), eq(7), eq(0), eq(100));
    }

    @Test
    void rejeita_janelaZero() {
        var uc = new VencimentoProximoUseCase(mock(RelatorioQueries.class));

        assertThatThrownBy(() -> uc.execute(UUID.randomUUID(), 0, 0, 100))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("positiva");
    }

    @Test
    void rejeita_janelaNegativa() {
        var uc = new VencimentoProximoUseCase(mock(RelatorioQueries.class));

        assertThatThrownBy(() -> uc.execute(UUID.randomUUID(), -5, 0, 100))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("positiva");
    }

    @Test
    void rejeita_janelaAcimaDoMaximo() {
        var uc = new VencimentoProximoUseCase(mock(RelatorioQueries.class));

        assertThatThrownBy(() -> uc.execute(UUID.randomUUID(), 366, 0, 100))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("máxima");
    }
}
