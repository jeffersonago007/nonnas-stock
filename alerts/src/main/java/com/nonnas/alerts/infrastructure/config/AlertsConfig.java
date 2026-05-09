package com.nonnas.alerts.infrastructure.config;

import com.nonnas.alerts.application.ports.AlertaConfigRepository;
import com.nonnas.alerts.application.ports.AlertaDisparadoRepository;
import com.nonnas.alerts.domain.AvaliadorAlertasService;
import com.nonnas.catalog.application.ports.InsumoFilialRepository;
import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class AlertsConfig {

    /**
     * Alerts é o primeiro módulo a depender de dois bounded contexts simultâneos
     * (inventory-core + catalog), cada um expondo seu próprio bean de
     * {@link Clock} ({@code inventoryClock}, {@code catalogClock}). Marcamos
     * este como {@code @Primary} para resolver a ambiguidade na injeção dos
     * use cases que precisam apenas de "um Clock qualquer".
     */
    @Bean
    @Primary
    public Clock alertsClock() { return Clock.systemUTC(); }

    @Bean
    public AvaliadorAlertasService avaliadorAlertasService(AlertaConfigRepository configRepo,
                                                           AlertaDisparadoRepository disparadoRepo,
                                                           SaldoLoteRepository saldoRepo,
                                                           InsumoFilialRepository insumoFilialRepo,
                                                           Clock clock) {
        return new AvaliadorAlertasService(configRepo, disparadoRepo, saldoRepo, insumoFilialRepo, clock);
    }
}
