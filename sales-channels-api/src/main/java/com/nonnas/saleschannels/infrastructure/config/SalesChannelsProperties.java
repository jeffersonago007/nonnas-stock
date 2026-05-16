package com.nonnas.saleschannels.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração transversal dos canais de venda.
 *
 * <p>O polling pode estar globalmente ligado/desligado via {@code enabled}.
 * Mesmo desligado, o endpoint manual {@code POST /api/v1/canais/{tipo}/poll-now}
 * (ADMIN) continua funcionando — útil em dev/CI.
 *
 * <p>{@code timeout} aplica ao RestClient usado pelos adapters HTTP.
 */
@ConfigurationProperties(prefix = "nonnas.canais")
public record SalesChannelsProperties(Polling polling, Http http) {

    public record Polling(boolean enabled, Duration interval, int batchSize) {}

    public record Http(Duration connectTimeout, Duration readTimeout) {}

    public SalesChannelsProperties {
        if (polling == null) {
            polling = new Polling(false, Duration.ofSeconds(30), 50);
        } else {
            polling = new Polling(
                    polling.enabled(),
                    polling.interval() != null ? polling.interval() : Duration.ofSeconds(30),
                    polling.batchSize() > 0 ? polling.batchSize() : 50);
        }
        if (http == null) {
            http = new Http(Duration.ofSeconds(5), Duration.ofSeconds(15));
        } else {
            http = new Http(
                    http.connectTimeout() != null ? http.connectTimeout() : Duration.ofSeconds(5),
                    http.readTimeout() != null ? http.readTimeout() : Duration.ofSeconds(15));
        }
    }
}
