package com.nonnas.inventory.infrastructure.config;

import com.nonnas.inventory.application.ports.LoteRepository;
import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import com.nonnas.inventory.domain.SelecionarLotesParaSaidaService;
import com.nonnas.inventory.domain.SelecionarLotesPorFefoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class InventoryConfig {

    @Bean
    public Clock inventoryClock() { return Clock.systemUTC(); }

    @Bean
    public SelecionarLotesPorFefoService fefoService(SaldoLoteRepository saldoRepo) {
        return new SelecionarLotesPorFefoService(saldoRepo);
    }

    @Bean
    public SelecionarLotesParaSaidaService selecionarLotesParaSaidaService(
            LoteRepository loteRepo,
            SaldoLoteRepository saldoRepo,
            SelecionarLotesPorFefoService fefo) {
        return new SelecionarLotesParaSaidaService(loteRepo, saldoRepo, fefo);
    }
}
