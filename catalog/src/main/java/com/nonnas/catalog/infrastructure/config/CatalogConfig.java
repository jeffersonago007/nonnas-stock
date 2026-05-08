package com.nonnas.catalog.infrastructure.config;

import com.nonnas.catalog.application.ports.ConversaoUnidadeRepository;
import com.nonnas.catalog.domain.ConversorUnidadeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CatalogConfig {

    @Bean
    public Clock catalogClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ConversorUnidadeService conversorUnidadeService(ConversaoUnidadeRepository repo) {
        return new ConversorUnidadeService(repo);
    }
}
