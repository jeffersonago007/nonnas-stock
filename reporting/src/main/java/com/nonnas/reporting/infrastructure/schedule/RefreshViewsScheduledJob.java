package com.nonnas.reporting.infrastructure.schedule;

import com.nonnas.reporting.application.RefreshViewsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Atualiza as views materializadas de reporting a cada 30 minutos.
 * Usa REFRESH MATERIALIZED VIEW CONCURRENTLY para não bloquear leituras.
 */
@Component
public class RefreshViewsScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshViewsScheduledJob.class);

    private final RefreshViewsUseCase useCase;

    public RefreshViewsScheduledJob(RefreshViewsUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(cron = "0 */30 * * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void refreshAgendado() {
        try {
            useCase.execute();
        } catch (RuntimeException e) {
            log.error("Falha no refresh agendado das views materializadas", e);
        }
    }
}
