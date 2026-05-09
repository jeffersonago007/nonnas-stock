package com.nonnas.reporting.application;

import com.nonnas.reporting.application.ports.RelatorioQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RefreshViewsUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshViewsUseCase.class);

    private final RelatorioQueries queries;

    public RefreshViewsUseCase(RelatorioQueries queries) {
        this.queries = queries;
    }

    public void execute() {
        long t0 = System.nanoTime();
        queries.refreshViewsMaterializadas();
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
        log.info("MVs de reporting atualizadas em {} ms", elapsedMs);
    }
}
