package com.nonnas.reporting.application;

import com.nonnas.reporting.application.ports.RelatorioQueries;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RefreshViewsUseCaseTest {

    @Test
    void delega_chamadaParaPort() {
        RelatorioQueries queries = mock(RelatorioQueries.class);

        new RefreshViewsUseCase(queries).execute();

        verify(queries).refreshViewsMaterializadas();
    }
}
